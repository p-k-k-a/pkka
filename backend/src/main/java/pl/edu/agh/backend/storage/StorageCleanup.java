package pl.edu.agh.backend.storage;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes files nothing points at any more. A file is uploaded the moment it is picked in a form and only
 * attached when the form is saved, so the bucket collects leftovers: uploads that were never saved, files a
 * record has since replaced, files of deleted records, pictures removed from a post.
 *
 * <p>There is no bookkeeping of who uses what. Each run compares the whole bucket with every reference in
 * the database, which also covers a file shared by two records (a picture copied between posts). A file is
 * not deleted when it is first found orphaned: the run only notes when that happened, and deletes the file
 * once it has stayed orphaned for the whole retention period. Attaching it again in the meantime clears the
 * note. So a deleted material or a replaced cover can still be put back for a week, and a fresh upload is
 * safe while its form is open.
 *
 * <p>What guards against wiping a bucket: the cleanup is off unless {@code app.storage.cleanup.mode} says
 * otherwise; the bucket carries the id of the one database allowed to clean it, and any other database
 * refuses; a database that references nothing at all, or a run that would delete more than
 * {@code max-deletions-per-run} files, deletes nothing and reports an error instead.
 */
@Slf4j
@Component
class StorageCleanup {

    /** Deletions that fail one after another before the run gives up, e.g. because storage is down. */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    enum Mode {
        OFF,
        /** Tracks orphans and logs what would be deleted, without deleting anything. */
        DRY_RUN,
        DELETE;

        static Mode parse(String value) {
            String name = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(
                        "app.storage.cleanup.mode must be off, dry-run or delete, not '" + value + "'", ex);
            }
        }
    }

    private final StorageService storage;
    private final StoredFileReferences references;
    private final StorageOrphanTracker tracker;
    private final Clock clock;
    private final Mode mode;
    private final Duration retention;
    private final int maxDeletionsPerRun;

    @Autowired
    StorageCleanup(
            StorageService storage,
            StoredFileReferences references,
            StorageOrphanTracker tracker,
            @Value("${app.storage.cleanup.mode:off}") String mode,
            @Value("${app.storage.cleanup.retention-days:7}") int retentionDays,
            @Value("${app.storage.cleanup.max-deletions-per-run:500}") int maxDeletionsPerRun) {
        this(
                storage,
                references,
                tracker,
                Clock.systemUTC(),
                Mode.parse(mode),
                Duration.ofDays(retentionDays),
                maxDeletionsPerRun);
    }

    StorageCleanup(
            StorageService storage,
            StoredFileReferences references,
            StorageOrphanTracker tracker,
            Clock clock,
            Mode mode,
            Duration retention,
            int maxDeletionsPerRun) {
        if (retention.compareTo(Duration.ofDays(1)) < 0) {
            throw new IllegalStateException("app.storage.cleanup.retention-days must be at least 1");
        }
        if (maxDeletionsPerRun < 1) {
            throw new IllegalStateException("app.storage.cleanup.max-deletions-per-run must be at least 1");
        }
        this.storage = storage;
        this.references = references;
        this.tracker = tracker;
        this.clock = clock;
        this.mode = mode;
        this.retention = retention;
        this.maxDeletionsPerRun = maxDeletionsPerRun;
    }

    @Scheduled(cron = "${app.storage.cleanup.cron:0 30 3 * * *}")
    void runScheduled() {
        if (mode == Mode.OFF || !storage.isConfigured()) {
            return;
        }
        try {
            int deleted = removeOrphans();
            log.info("Storage cleanup ({}) finished, {} file(s) deleted", mode, deleted);
        } catch (RuntimeException ex) {
            // Nothing is deleted on a half-read bucket or database; the next run starts over.
            log.error("Storage cleanup stopped without deleting anything more: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Notes the files that are orphaned now and deletes those that have been for the whole retention period.
     *
     * @return how many files were deleted
     * @throws IllegalStateException when one of the guards described on the class refuses the run
     */
    int removeOrphans() {
        Instant now = clock.instant();
        requireOwnership();

        // The bucket first, the database second: a file attached in between is then already referenced.
        Map<String, Instant> issued = storage.listObjects().stream()
                .filter(object -> StorageService.isIssuedKey(object.key()))
                .collect(Collectors.toMap(StoredObject::key, StoredObject::lastModified, (first, second) -> first));
        Set<String> referenced = references.referencedKeys();
        if (referenced.isEmpty() && !issued.isEmpty()) {
            throw new IllegalStateException("the database references no file at all while the bucket holds "
                    + issued.size() + "; refusing to treat them all as orphans");
        }

        Set<String> orphans = issued.keySet().stream()
                .filter(key -> !referenced.contains(key))
                .collect(Collectors.toSet());
        Map<String, Instant> orphanedSince = tracker.sync(orphans, now);

        Instant cutoff = now.minus(retention);
        List<String> due = orphanedSince.entrySet().stream()
                .filter(entry -> !entry.getValue().isAfter(cutoff))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (due.size() > maxDeletionsPerRun) {
            throw new IllegalStateException(due.size() + " files are due for deletion, more than the limit of "
                    + maxDeletionsPerRun + " per run; check that this database owns the bucket, then raise "
                    + "app.storage.cleanup.max-deletions-per-run for one run if the number is right");
        }

        int deleted = 0;
        int consecutiveFailures = 0;
        for (String key : due) {
            // Asked again right before deleting: the snapshot above ages while this loop talks to storage.
            if (references.isReferenced(key)) {
                continue;
            }
            if (mode == Mode.DRY_RUN) {
                log.info(
                        "Storage cleanup would delete {} (uploaded {}, orphaned since {})",
                        key,
                        issued.get(key),
                        orphanedSince.get(key));
                continue;
            }
            try {
                storage.delete(key);
                tracker.forget(key);
                deleted++;
                consecutiveFailures = 0;
                log.info(
                        "Storage cleanup deleted {} (uploaded {}, orphaned since {})",
                        key,
                        issued.get(key),
                        orphanedSince.get(key));
            } catch (RuntimeException ex) {
                log.warn("Storage cleanup could not delete {}: {}", key, ex.getMessage());
                if (++consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    // The scheduler has one thread; do not hold it for every remaining file.
                    throw new IllegalStateException(
                            "storage keeps failing, giving up after " + deleted + " deletion(s)", ex);
                }
            }
        }
        return deleted;
    }

    /**
     * The bucket remembers which database cleans it. The first database to run a cleanup claims it; any
     * other one (a staging backend pointed at the production bucket, a local database) is turned away, because
     * to it every file would look orphaned.
     */
    private void requireOwnership() {
        String databaseId = tracker.databaseId();
        Optional<String> owner = storage.readCleanupOwner();
        if (owner.isEmpty()) {
            storage.writeCleanupOwner(databaseId);
            log.info("Storage cleanup claimed the bucket for database {}", databaseId);
            return;
        }
        if (!owner.get().equals(databaseId)) {
            throw new IllegalStateException("the bucket is cleaned by database " + owner.get() + ", this is "
                    + databaseId + "; a bucket must not be cleaned from two databases");
        }
    }
}
