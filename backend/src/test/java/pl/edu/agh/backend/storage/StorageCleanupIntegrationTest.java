package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.storage.StorageCleanup.Mode;
import pl.edu.agh.backend.storage.StorageOrphanTracker.CleanupState;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** The cleanup against a real database: only the bucket is a stand-in. */
@SpringBootTest
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class StorageCleanupIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-16T03:30:00Z");
    private static final Instant LONG_AGO = NOW.minus(Duration.ofDays(400));

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private StoredFileReferences references;

    @Autowired
    private StorageOrphanTracker tracker;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    private final StorageService bucket = mock(StorageService.class);

    private final String inUse = "materials/" + UUID.randomUUID() + ".pdf";

    /** The marker object of the stand-in bucket. */
    private final AtomicReference<String> marker = new AtomicReference<>();

    @BeforeEach
    void aBucketOwnedByThisDatabaseWithOneFileInUse() {
        CleanupState state = tracker.state();
        marker.set(state.databaseId() + ":" + state.generation());
        when(bucket.readCleanupOwner()).thenAnswer(call -> Optional.ofNullable(marker.get()));
        doAnswer(call -> {
                    marker.set(call.getArgument(0));
                    return null;
                })
                .when(bucket)
                .writeCleanupOwner(anyString());
        doAnswer(call -> {
                    marker.set(call.getArgument(0));
                    return null;
                })
                .when(bucket)
                .claimCleanupOwner(anyString());
        insertMaterial(inUse);
    }

    @Test
    void aMaterialDeletedByMistakeCanBePutBackForAWeekAndIsDeletedAfterIt() {
        String key = "materials/" + UUID.randomUUID() + ".pdf";
        UUID material = insertMaterial(key);
        bucketHolds(key);

        assertThat(nightlyRuns(0, 0)).isZero();
        assertThat(orphanedSince(key)).isNull();

        // the admin deletes a year-old material; that night its file is only noted
        jdbc.update("DELETE FROM materials WHERE id = ?", material);
        assertThat(nightlyRuns(1, 3)).isZero();
        assertThat(orphanedSince(key)).isEqualTo(NOW.plus(Duration.ofDays(1)));

        // put back three days later: the note is gone, and so is the countdown
        UUID restored = insertMaterial(key);
        assertThat(nightlyRuns(4, 4)).isZero();
        assertThat(orphanedSince(key)).isNull();

        // deleted for good: seven more days, counted from the night it was found orphaned again
        jdbc.update("DELETE FROM materials WHERE id = ?", restored);
        assertThat(nightlyRuns(5, 11)).isZero();
        assertThat(orphanedSince(key)).isEqualTo(NOW.plus(Duration.ofDays(5)));
        verify(bucket, never()).delete(anyString());

        assertThat(nightlyRuns(12, 12)).isEqualTo(1);
        verify(bucket).delete(key);
        assertThat(orphanedSince(key)).isNull();
    }

    @Test
    void aNoteLeftFromBeforeABreakInTheRunsDoesNotCountAgainstTheFile() {
        String key = "materials/" + UUID.randomUUID() + ".pdf";
        bucketHolds(key);
        assertThat(nightlyRuns(0, 1)).isZero();

        // the cleanup is switched off for a month; the first run back must not delete on a month-old note
        assertThat(nightlyRuns(31, 31)).isZero();
        assertThat(orphanedSince(key)).isEqualTo(NOW.plus(Duration.ofDays(31)));
        assertThat(nightlyRuns(32, 37)).isZero();
        verify(bucket, never()).delete(anyString());

        assertThat(nightlyRuns(38, 38)).isEqualTo(1);
    }

    @Test
    void everyRunIsCountedAndACopyOfTheDatabaseFallsBehind() {
        bucketHolds("materials/" + UUID.randomUUID() + ".pdf");
        long before = tracker.state().generation();

        nightlyRuns(0, 2);
        assertThat(tracker.state().generation()).isEqualTo(before + 3);
        assertThat(tracker.state().lastRunAt()).isEqualTo(NOW.plus(Duration.ofDays(2)));
        assertThat(marker.get()).isEqualTo(tracker.state().databaseId() + ":" + (before + 3));

        // what a copy taken now sees after the original has run three more nights
        marker.set(tracker.state().databaseId() + ":" + (before + 6));
        assertThatThrownBy(() -> cleanupAt(NOW.plus(Duration.ofDays(3))).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a copy or was restored");
        assertThat(tracker.state().generation()).isEqualTo(before + 3);
    }

    @Test
    void claimingAnUnclaimedBucketStartsTheNotesAfresh() {
        String key = "materials/" + UUID.randomUUID() + ".pdf";
        bucketHolds(key);
        jdbc.update(
                "INSERT INTO storage_orphans (object_key, first_seen_at) VALUES (?, ?)", key, Timestamp.from(LONG_AGO));
        marker.set(null);

        assertThat(cleanupAt(NOW).removeOrphans()).isZero();

        assertThat(marker.get())
                .isEqualTo(tracker.state().databaseId() + ":" + tracker.state().generation());
        assertThat(orphanedSince(key)).isEqualTo(NOW);
        verify(bucket, never()).delete(anyString());
    }

    @Test
    void aPictureStillMentionedInAPostIsNeverNoted() {
        String picture = "posts/images/" + UUID.randomUUID() + ".png";
        User author = new User();
        author.setKeycloakId(UUID.randomUUID().toString());
        author = userRepository.saveAndFlush(author);
        jdbc.update(
                "INSERT INTO posts (id, title, slug, content, author_id, status, created_at, updated_at)"
                        + " VALUES (gen_random_uuid(), 'Relacja', ?, ?, ?, 'DRAFT', now(), now())",
                "relacja-" + UUID.randomUUID(),
                "tekst ![x](" + picture + ") tekst",
                author.getId());
        bucketHolds(picture);

        assertThat(cleanupAt(NOW).removeOrphans()).isZero();
        assertThat(cleanupAt(NOW.plus(Duration.ofDays(30))).removeOrphans()).isZero();

        assertThat(orphanedSince(picture)).isNull();
        verify(bucket, never()).delete(anyString());
    }

    @Test
    void theIdOfTheDatabaseIsStableAndABucketOfAnotherOneIsRefused() {
        assertThat(tracker.state().databaseId())
                .isEqualTo(tracker.state().databaseId())
                .hasSize(36);

        marker.set(UUID.randomUUID() + ":0");
        assertThatThrownBy(() -> cleanupAt(NOW).removeOrphans()).isInstanceOf(IllegalStateException.class);
        verify(bucket, never()).listObjects();
    }

    private UUID insertMaterial(String url) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO materials (id, title, type, url, audience, created_at, updated_at)"
                        + " VALUES (?, 'Slajdy', 'PRESENTATION', ?, 'ALL_ALUMNI', now(), now())",
                id,
                url);
        return id;
    }

    private void bucketHolds(String key) {
        when(bucket.listObjects())
                .thenReturn(List.of(new StoredObject(inUse, LONG_AGO), new StoredObject(key, LONG_AGO)));
    }

    private Instant orphanedSince(String key) {
        List<Instant> found = jdbc.query(
                "SELECT first_seen_at FROM storage_orphans WHERE object_key = ?",
                (row, index) -> row.getTimestamp(1).toInstant(),
                key);
        return found.isEmpty() ? null : found.get(0);
    }

    /** One run a night from day {@code from} to day {@code to}; returns how many files they deleted. */
    private int nightlyRuns(int from, int to) {
        int deleted = 0;
        for (int day = from; day <= to; day++) {
            deleted += cleanupAt(NOW.plus(Duration.ofDays(day))).removeOrphans();
        }
        return deleted;
    }

    private StorageCleanup cleanupAt(Instant now) {
        return new StorageCleanup(
                bucket,
                references,
                tracker,
                Clock.fixed(now, ZoneOffset.UTC),
                Mode.DELETE,
                Duration.ofDays(7),
                500,
                50);
    }
}
