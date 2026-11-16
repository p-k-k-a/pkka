package pl.edu.agh.backend.storage;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
 * <p>There is no bookkeeping of who uses what. Instead the whole bucket is compared with every reference in
 * the database, which also covers a file shared by two records (a picture copied between posts). A file is
 * only removed once it is older than the retention period: that keeps a fresh upload safe while its form is
 * still open, and leaves time to notice a mistake.
 *
 * <p>Off unless {@code app.storage.cleanup.enabled} is set. It deletes whatever the database does not know,
 * so it must never run against a bucket that another environment's database is the owner of.
 */
@Slf4j
@Component
class StorageCleanup {

    private final StorageService storage;
    private final StoredFileReferences references;
    private final Clock clock;
    private final boolean enabled;
    private final Duration retention;
    private final int maxDeletionsPerRun;

    @Autowired
    StorageCleanup(
            StorageService storage,
            StoredFileReferences references,
            @Value("${app.storage.cleanup.enabled:false}") boolean enabled,
            @Value("${app.storage.cleanup.retention-days:7}") int retentionDays,
            @Value("${app.storage.cleanup.max-deletions-per-run:500}") int maxDeletionsPerRun) {
        this(storage, references, Clock.systemUTC(), enabled, Duration.ofDays(retentionDays), maxDeletionsPerRun);
    }

    StorageCleanup(
            StorageService storage,
            StoredFileReferences references,
            Clock clock,
            boolean enabled,
            Duration retention,
            int maxDeletionsPerRun) {
        this.storage = storage;
        this.references = references;
        this.clock = clock;
        this.enabled = enabled;
        this.retention = retention;
        this.maxDeletionsPerRun = maxDeletionsPerRun;
    }

    @Scheduled(cron = "${app.storage.cleanup.cron:0 30 3 * * *}")
    void runScheduled() {
        if (!enabled || !storage.isConfigured()) {
            return;
        }
        try {
            int deleted = removeOrphans();
            log.info("Storage cleanup removed {} orphaned file(s)", deleted);
        } catch (RuntimeException ex) {
            // Nothing is deleted on a half-read bucket or database; the next run starts over.
            log.error("Storage cleanup failed, nothing more is deleted in this run", ex);
        }
    }

    /** Deletes what is old enough and referenced by nothing, and returns how many files that was. */
    int removeOrphans() {
        Instant cutoff = clock.instant().minus(retention);
        // The bucket first, the database second: a file attached in between is then already referenced.
        List<StoredObject> objects = storage.listObjects();
        Set<String> referenced = references.referencedKeys();

        List<String> orphans = objects.stream()
                .filter(object -> object.lastModified().isBefore(cutoff))
                .map(StoredObject::key)
                // Anything that does not look like one of our uploads was put there by someone else.
                .filter(StorageService::isIssuedKey)
                .filter(key -> !referenced.contains(key))
                .toList();
        if (orphans.size() > maxDeletionsPerRun) {
            log.warn(
                    "Storage cleanup found {} orphaned files, deleting only {} in this run",
                    orphans.size(),
                    maxDeletionsPerRun);
        }

        int deleted = 0;
        for (String key : orphans.subList(0, Math.min(orphans.size(), maxDeletionsPerRun))) {
            try {
                storage.delete(key);
                deleted++;
            } catch (RuntimeException ex) {
                log.warn("Storage cleanup could not delete {}: {}", key, ex.getMessage());
            }
        }
        return deleted;
    }
}
