package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @BeforeEach
    void aBucketOwnedByThisDatabaseWithOneFileInUse() {
        when(bucket.readCleanupOwner()).thenReturn(Optional.of(tracker.databaseId()));
        insertMaterial(inUse);
    }

    @Test
    void aMaterialDeletedByMistakeCanBePutBackForAWeekAndIsDeletedAfterIt() {
        String key = "materials/" + UUID.randomUUID() + ".pdf";
        UUID material = insertMaterial(key);
        bucketHolds(key);

        assertThat(cleanupAt(NOW).removeOrphans()).isZero();
        assertThat(orphanedSince(key)).isNull();

        // the admin deletes a year-old material; that night its file is only noted
        jdbc.update("DELETE FROM materials WHERE id = ?", material);
        assertThat(cleanupAt(NOW.plus(Duration.ofDays(1))).removeOrphans()).isZero();
        assertThat(orphanedSince(key)).isEqualTo(NOW.plus(Duration.ofDays(1)));

        // put back three days later: the note is gone, and so is the countdown
        UUID restored = insertMaterial(key);
        assertThat(cleanupAt(NOW.plus(Duration.ofDays(4))).removeOrphans()).isZero();
        assertThat(orphanedSince(key)).isNull();

        // deleted for good: seven more days, counted from the night it was found orphaned again
        jdbc.update("DELETE FROM materials WHERE id = ?", restored);
        assertThat(cleanupAt(NOW.plus(Duration.ofDays(5))).removeOrphans()).isZero();
        assertThat(cleanupAt(NOW.plus(Duration.ofDays(11))).removeOrphans()).isZero();
        verify(bucket, never()).delete(anyString());

        assertThat(cleanupAt(NOW.plus(Duration.ofDays(12))).removeOrphans()).isEqualTo(1);
        verify(bucket).delete(key);
        assertThat(orphanedSince(key)).isNull();
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
        assertThat(tracker.databaseId()).isEqualTo(tracker.databaseId()).hasSize(36);

        when(bucket.readCleanupOwner()).thenReturn(Optional.of(UUID.randomUUID().toString()));
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

    private StorageCleanup cleanupAt(Instant now) {
        return new StorageCleanup(
                bucket, references, tracker, Clock.fixed(now, ZoneOffset.UTC), Mode.DELETE, Duration.ofDays(7), 500);
    }
}
