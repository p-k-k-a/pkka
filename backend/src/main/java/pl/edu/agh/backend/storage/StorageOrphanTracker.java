package pl.edu.agh.backend.storage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** What the cleanup remembers between runs: since when each file has been orphaned, and its own runs. */
@Repository
@RequiredArgsConstructor
class StorageOrphanTracker {

    private static final int BATCH_SIZE = 500;

    /**
     * @param databaseId identifies this database for good; a copy or a restore keeps it
     * @param generation how many deleting runs this database has made
     * @param lastRunAt when the orphans were last noted, {@code null} before the first run
     */
    record CleanupState(String databaseId, long generation, Instant lastRunAt) {}

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public CleanupState state() {
        return jdbc.queryForObject(
                "SELECT id::text, generation, last_run_at FROM storage_cleanup_owner",
                (row, index) -> new CleanupState(
                        row.getString(1),
                        row.getLong(2),
                        row.getTimestamp(3) == null ? null : row.getTimestamp(3).toInstant()));
    }

    /** Records that deleting run number {@code generation} is under way; never counts backwards. */
    @Transactional
    public void advanceGeneration(long generation) {
        jdbc.update("UPDATE storage_cleanup_owner SET generation = GREATEST(generation, ?)", generation);
    }

    /** The files noted as orphaned by earlier runs. */
    @Transactional(readOnly = true)
    public Set<String> notedKeys() {
        return new HashSet<>(jdbc.queryForList("SELECT object_key FROM storage_orphans", String.class));
    }

    @Transactional
    public void markRun(Instant now) {
        jdbc.update("UPDATE storage_cleanup_owner SET last_run_at = ?", Timestamp.from(now));
    }

    /** Makes every noted file count as orphaned only since {@code now}. */
    @Transactional
    public void restartCountdowns(Instant now) {
        jdbc.update("UPDATE storage_orphans SET first_seen_at = ?", Timestamp.from(now));
    }

    /**
     * Brings the notes in line with the files that are orphaned now: a newly orphaned file is noted as
     * orphaned since {@code now}, a file that is referenced again (or gone) is forgotten, and one that was
     * already noted keeps its original time.
     *
     * @return every currently orphaned file with the time it was first seen that way
     */
    @Transactional
    public Map<String, Instant> sync(Set<String> orphans, Instant now) {
        Map<String, Instant> noted = new HashMap<>();
        jdbc.query("SELECT object_key, first_seen_at FROM storage_orphans", row -> {
            noted.put(row.getString(1), row.getTimestamp(2).toInstant());
        });

        List<Object[]> noLongerOrphaned = new ArrayList<>();
        noted.keySet().removeIf(key -> {
            if (orphans.contains(key)) {
                return false;
            }
            noLongerOrphaned.add(new Object[] {key});
            return true;
        });
        List<Object[]> newlyOrphaned = new ArrayList<>();
        for (String key : orphans) {
            if (noted.putIfAbsent(key, now) == null) {
                newlyOrphaned.add(new Object[] {key, Timestamp.from(now)});
            }
        }

        jdbc.batchUpdate("DELETE FROM storage_orphans WHERE object_key = ?", noLongerOrphaned);
        for (int from = 0; from < newlyOrphaned.size(); from += BATCH_SIZE) {
            // Another backend instance running the same cleanup may have noted the file a moment ago.
            jdbc.batchUpdate(
                    "INSERT INTO storage_orphans (object_key, first_seen_at) VALUES (?, ?)"
                            + " ON CONFLICT (object_key) DO NOTHING",
                    newlyOrphaned.subList(from, Math.min(from + BATCH_SIZE, newlyOrphaned.size())));
        }
        return noted;
    }

    /** Drops the note of a file that has just been deleted. */
    @Transactional
    public void forget(String key) {
        jdbc.update("DELETE FROM storage_orphans WHERE object_key = ?", key);
    }
}
