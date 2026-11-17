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
import pl.edu.agh.backend.storage.StorageOrphanTracker.CleanupState;

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
 * safe while its form is open. The notes are only trusted while runs follow one another: after a break
 * longer than {@link #MAX_GAP_BETWEEN_RUNS} every countdown starts again.
 *
 * <p>To a database that does not belong to the bucket every file looks orphaned, so several things have to
 * agree before anything is deleted:
 *
 * <ul>
 *   <li>the cleanup is off unless {@code app.storage.cleanup.mode} says otherwise, and a dry run never
 *       writes to the bucket;
 *   <li>the bucket carries a marker with the id of the database that cleans it and the number of its last
 *       run. Another database is refused, and so is a copy of the right one (a dump restored elsewhere, or
 *       the original restored from a backup), because its run number has fallen behind. Deleting the marker
 *       object by hand lets the next run claim the bucket and start its notes from nothing;
 *   <li>a database that references none of the files in the bucket, or would call too large a share of
 *       them orphaned, is not believed;
 *   <li>a run that would delete more than {@code max-deletions-per-run} files deletes none.
 * </ul>
 *
 * A refusal deletes nothing and is logged as an error.
 */
@Slf4j
@Component
class StorageCleanup {

    /** Deletions that fail one after another before the run gives up, e.g. because storage is down. */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    /** Twice the nightly rhythm. A cron that runs less often than this never gets to delete anything. */
    static final Duration MAX_GAP_BETWEEN_RUNS = Duration.ofDays(2);

    /** Below this many files a share of them says nothing. */
    private static final int MIN_FILES_FOR_ORPHAN_SHARE = 20;

    enum Mode {
        OFF,
        /** Tracks orphans and logs what would be deleted, without deleting or writing anything in the bucket. */
        DRY_RUN,
        DELETE;

        static Mode parse(String value) {
            String name = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if (name.isEmpty()) {
                return OFF;
            }
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(
                        "app.storage.cleanup.mode must be off, dry-run or delete, not '" + value + "'", ex);
            }
        }
    }

    /** What the bucket's marker says: which database cleans it and how many runs it has made. */
    private record Marker(String databaseId, long generation) {

        static Marker parse(String text) {
            int separator = text.lastIndexOf(':');
            try {
                return new Marker(text.substring(0, separator), Long.parseLong(text.substring(separator + 1)));
            } catch (RuntimeException ex) {
                throw new IllegalStateException(
                        "the bucket's " + StorageService.CLEANUP_OWNER_KEY + " marker is unreadable: '" + text + "'",
                        ex);
            }
        }

        String text() {
            return databaseId + ":" + generation;
        }
    }

    private final StorageService storage;
    private final StoredFileReferences references;
    private final StorageOrphanTracker tracker;
    private final Clock clock;
    private final Mode mode;
    private final Duration retention;
    private final int maxDeletionsPerRun;
    private final int maxOrphanPercent;

    @Autowired
    StorageCleanup(
            StorageService storage,
            StoredFileReferences references,
            StorageOrphanTracker tracker,
            @Value("${app.storage.cleanup.mode:off}") String mode,
            @Value("${app.storage.cleanup.retention-days:7}") int retentionDays,
            @Value("${app.storage.cleanup.max-deletions-per-run:500}") int maxDeletionsPerRun,
            @Value("${app.storage.cleanup.max-orphan-percent:50}") int maxOrphanPercent) {
        this(
                storage,
                references,
                tracker,
                Clock.systemUTC(),
                Mode.parse(mode),
                Duration.ofDays(retentionDays),
                maxDeletionsPerRun,
                maxOrphanPercent);
        if (this.mode != Mode.OFF && !storage.isConfigured()) {
            log.warn("Storage cleanup is set to {} but file storage is not configured; it will not run", this.mode);
        } else {
            log.info("Storage cleanup mode: {}", this.mode);
        }
    }

    StorageCleanup(
            StorageService storage,
            StoredFileReferences references,
            StorageOrphanTracker tracker,
            Clock clock,
            Mode mode,
            Duration retention,
            int maxDeletionsPerRun,
            int maxOrphanPercent) {
        if (retention.compareTo(Duration.ofDays(1)) < 0) {
            throw new IllegalStateException("app.storage.cleanup.retention-days must be at least 1");
        }
        if (maxDeletionsPerRun < 1) {
            throw new IllegalStateException("app.storage.cleanup.max-deletions-per-run must be at least 1");
        }
        if (maxOrphanPercent < 1 || maxOrphanPercent > 100) {
            throw new IllegalStateException("app.storage.cleanup.max-orphan-percent must be between 1 and 100");
        }
        this.storage = storage;
        this.references = references;
        this.tracker = tracker;
        this.clock = clock;
        this.mode = mode;
        this.retention = retention;
        this.maxDeletionsPerRun = maxDeletionsPerRun;
        this.maxOrphanPercent = maxOrphanPercent;
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
        CleanupState state = tracker.state();
        Optional<Marker> marker = storage.readCleanupOwner().map(Marker::parse);
        marker.ifPresent(found -> requireOwnership(found, state));

        // The bucket first, the database second: a file attached in between is then already referenced.
        Map<String, Instant> issued = storage.listObjects().stream()
                .filter(object -> StorageService.isIssuedKey(object.key()))
                .collect(Collectors.toMap(StoredObject::key, StoredObject::lastModified, (first, second) -> first));
        Set<String> referenced = references.referencedKeys();
        Set<String> orphans = issued.keySet().stream()
                .filter(key -> !referenced.contains(key))
                .collect(Collectors.toSet());
        requirePlausible(issued.size(), orphans.size());

        // Only now, with every check passed, is anything written: to the bucket in delete mode alone.
        boolean claiming = marker.isEmpty() && mode == Mode.DELETE;
        if (mode == Mode.DELETE) {
            Marker next = new Marker(state.databaseId(), tracker.nextGeneration());
            if (claiming) {
                claim(next);
            } else {
                storage.writeCleanupOwner(next.text());
            }
        }
        if (claiming) {
            // Notes taken before this database owned the bucket (a dry run, an earlier owner) prove nothing.
            tracker.restartCountdowns(now);
        } else if (state.lastRunAt() != null
                && Duration.between(state.lastRunAt(), now).compareTo(MAX_GAP_BETWEEN_RUNS) > 0) {
            // Nobody watched in between: a noted file may have been attached and dropped again yesterday.
            log.warn(
                    "Storage cleanup last ran {}, more than {} days ago; every file's retention starts again",
                    state.lastRunAt(),
                    MAX_GAP_BETWEEN_RUNS.toDays());
            tracker.restartCountdowns(now);
        }
        Map<String, Instant> orphanedSince = tracker.sync(orphans, now);
        tracker.markRun(now);

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
        return delete(due, issued, orphanedSince);
    }

    private int delete(List<String> due, Map<String, Instant> issued, Map<String, Instant> orphanedSince) {
        int deleted = 0;
        int consecutiveFailures = 0;
        for (String key : due) {
            // Asked again right before deleting: the snapshot ages while this loop talks to storage.
            if (references.isReferenced(key)) {
                log.info("Storage cleanup kept {}: it was attached again while the run was under way", key);
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
            } catch (RuntimeException ex) {
                log.warn("Storage cleanup could not delete {}: {}", key, ex.getMessage());
                if (++consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    // The scheduler has one thread; do not hold it for every remaining file.
                    throw new IllegalStateException(
                            "storage keeps failing, giving up after " + deleted + " deletion(s)", ex);
                }
                continue;
            }
            deleted++;
            consecutiveFailures = 0;
            log.info(
                    "Storage cleanup deleted {} (uploaded {}, orphaned since {})",
                    key,
                    issued.get(key),
                    orphanedSince.get(key));
            try {
                tracker.forget(key);
            } catch (RuntimeException ex) {
                // The file is gone, so the next run drops the note anyway.
                log.warn("Storage cleanup deleted {} but could not drop its note: {}", key, ex.getMessage());
            }
        }
        return deleted;
    }

    /**
     * The marker must name this database and its latest run. A copy of the database carries the same id but
     * stops counting runs with the original, so it falls behind; being one ahead is the trace of a run that
     * died between counting itself and writing the marker.
     */
    private static void requireOwnership(Marker marker, CleanupState state) {
        if (!marker.databaseId().equals(state.databaseId())) {
            throw new IllegalStateException("the bucket is cleaned by database " + marker.databaseId() + ", this is "
                    + state.databaseId() + "; a bucket must not be cleaned from two databases");
        }
        long ahead = state.generation() - marker.generation();
        if (ahead != 0 && ahead != 1) {
            throw new IllegalStateException("the bucket was last cleaned in run " + marker.generation()
                    + " but this database remembers run " + state.generation() + ", so it is a copy or was "
                    + "restored from a backup; if it is the one that should clean the bucket, delete the "
                    + StorageService.CLEANUP_OWNER_KEY + " object from the bucket and it will be claimed anew");
        }
    }

    /** A database that knows the bucket references at least some of it and calls only part of it orphaned. */
    private void requirePlausible(int files, int orphans) {
        if (files > 0 && orphans == files) {
            throw new IllegalStateException("the database references none of the " + files
                    + " file(s) in the bucket; refusing to treat them all as orphans");
        }
        if (files >= MIN_FILES_FOR_ORPHAN_SHARE && (long) orphans * 100 > (long) maxOrphanPercent * files) {
            throw new IllegalStateException(orphans + " of the " + files + " files in the bucket look orphaned, "
                    + "more than " + maxOrphanPercent + "%; check that this database owns the bucket, then raise "
                    + "app.storage.cleanup.max-orphan-percent for one run if the number is right");
        }
    }

    /** Writes the first marker, unless another database gets there first. */
    private void claim(Marker marker) {
        storage.claimCleanupOwner(marker.text());
        Optional<String> written = storage.readCleanupOwner();
        if (written.isEmpty() || !written.get().equals(marker.text())) {
            throw new IllegalStateException(
                    "another database claimed the bucket at the same moment (marker: " + written.orElse("none") + ")");
        }
        log.info("Storage cleanup claimed the bucket for database {}", marker.databaseId());
    }
}
