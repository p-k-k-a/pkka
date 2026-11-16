package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.backend.storage.StorageCleanup.Mode;

@ExtendWith(MockitoExtension.class)
class StorageCleanupTest {

    private static final Instant NOW = Instant.parse("2026-11-16T03:30:00Z");
    private static final Duration RETENTION = Duration.ofDays(7);
    private static final Instant LONG_AGO = NOW.minus(Duration.ofDays(400));
    private static final String DATABASE_ID = "0f3a1c8e-0000-4000-8000-00000000000a";
    private static final String IN_USE = "materials/0f3a1c8e-0000-4000-8000-0000000000ff.pdf";

    @Mock
    private StorageService storage;

    @Mock
    private StoredFileReferences references;

    @Mock
    private StorageOrphanTracker tracker;

    /** What the tracker "remembers": key -> orphaned since. */
    private final Map<String, Instant> noted = new HashMap<>();

    @BeforeEach
    void aBucketThisDatabaseOwns() {
        lenient().when(tracker.databaseId()).thenReturn(DATABASE_ID);
        lenient().when(storage.readCleanupOwner()).thenReturn(Optional.of(DATABASE_ID));
        lenient().when(references.referencedKeys()).thenReturn(Set.of(IN_USE));
        lenient().when(references.isReferenced(anyString())).thenReturn(false);
        lenient().when(tracker.sync(any(), any())).thenAnswer(call -> {
            Set<String> orphans = call.getArgument(0);
            noted.keySet().retainAll(orphans);
            orphans.forEach(key -> noted.putIfAbsent(key, call.getArgument(1)));
            return new HashMap<>(noted);
        });
    }

    @Test
    void aFileIsDeletedOnlyAfterStayingOrphanedForTheWholeRetention() {
        String key = material();
        bucketHolds(key);

        // first seen orphaned tonight, however old the upload is
        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isZero();
        assertThat(cleanup(Mode.DELETE, NOW.plus(RETENTION).minusSeconds(1)).removeOrphans())
                .isZero();
        verify(storage, never()).delete(anyString());

        assertThat(cleanup(Mode.DELETE, NOW.plus(RETENTION)).removeOrphans()).isEqualTo(1);
        verify(storage).delete(key);
        verify(tracker).forget(key);
    }

    @Test
    void aFileAttachedAgainStartsOverWhenItIsOrphanedNextTime() {
        String key = material();
        bucketHolds(key);
        cleanup(Mode.DELETE, NOW).removeOrphans();

        when(references.referencedKeys()).thenReturn(Set.of(IN_USE, key));
        cleanup(Mode.DELETE, NOW.plus(Duration.ofDays(6))).removeOrphans();
        when(references.referencedKeys()).thenReturn(Set.of(IN_USE));

        // orphaned again on day 8: the first six days do not count
        assertThat(cleanup(Mode.DELETE, NOW.plus(Duration.ofDays(8))).removeOrphans())
                .isZero();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void aFileAttachedWhileTheRunIsUnderWayIsKept() {
        String attachedMeanwhile = material();
        String orphan = material();
        noted.put(attachedMeanwhile, LONG_AGO);
        noted.put(orphan, LONG_AGO);
        bucketHolds(attachedMeanwhile, orphan);
        when(references.isReferenced(attachedMeanwhile)).thenReturn(true);

        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isEqualTo(1);

        verify(storage).delete(orphan);
        verify(storage, never()).delete(attachedMeanwhile);
    }

    @Test
    void whatIsReferencedOrNotOursIsNeverEvenNoted() {
        when(storage.listObjects())
                .thenReturn(List.of(
                        new StoredObject(IN_USE, LONG_AGO),
                        new StoredObject("backups/2026-11-01.zip", LONG_AGO),
                        new StoredObject("materials/notes.pdf", LONG_AGO),
                        new StoredObject(StorageService.CLEANUP_OWNER_KEY, LONG_AGO)));

        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isZero();

        verify(tracker).sync(Set.of(), NOW);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void aDryRunTracksAndReportsButDeletesNothing() {
        String key = material();
        noted.put(key, LONG_AGO);
        bucketHolds(key);

        assertThat(cleanup(Mode.DRY_RUN, NOW).removeOrphans()).isZero();

        verify(tracker).sync(Set.of(key), NOW);
        verify(storage, never()).delete(anyString());
        verify(tracker, never()).forget(anyString());
    }

    @Test
    void anUnclaimedBucketIsClaimedByTheFirstCleanup() {
        when(storage.readCleanupOwner()).thenReturn(Optional.empty());
        bucketHolds();

        cleanup(Mode.DELETE, NOW).removeOrphans();

        verify(storage).writeCleanupOwner(DATABASE_ID);
    }

    @Test
    void aBucketCleanedByAnotherDatabaseIsLeftAlone() {
        when(storage.readCleanupOwner()).thenReturn(Optional.of("0f3a1c8e-0000-4000-8000-00000000000b"));

        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("two databases");

        verify(storage, never()).listObjects();
        verify(storage, never()).writeCleanupOwner(anyString());
        verifyNoInteractions(references);
        verify(tracker, never()).sync(any(), any());
    }

    @Test
    void aDatabaseThatReferencesNothingIsNotBelieved() {
        String key = material();
        noted.put(key, LONG_AGO);
        bucketHolds(key);
        when(references.referencedKeys()).thenReturn(Set.of());

        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("references no file");

        verify(tracker, never()).sync(any(), any());
        verify(storage, never()).delete(anyString());
    }

    @Test
    void moreDueFilesThanTheLimitDeletesNoneOfThem() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            keys.add(material());
            noted.put(keys.get(i), LONG_AGO);
        }
        bucketHolds(keys.toArray(String[]::new));

        assertThatThrownBy(() -> new StorageCleanup(
                                storage,
                                references,
                                tracker,
                                Clock.fixed(NOW, ZoneOffset.UTC),
                                Mode.DELETE,
                                RETENTION,
                                2)
                        .removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("more than the limit of 2");

        verify(storage, never()).delete(anyString());
    }

    @Test
    void oneFailedDeletionDoesNotStopTheRestButARunOfThemDoes() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            keys.add("materials/0f3a1c8e-0000-4000-8000-00000000000" + i + ".pdf");
            noted.put(keys.get(i), LONG_AGO);
        }
        bucketHolds(keys.toArray(String[]::new));
        doThrow(new IllegalStateException("R2 said no")).when(storage).delete(keys.get(0));

        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isEqualTo(5);
        verify(tracker, never()).forget(keys.get(0));

        // storage down: three failures in a row end the run instead of timing out on every file
        doThrow(new IllegalStateException("R2 is down")).when(storage).delete(anyString());
        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("keeps failing");
        verify(storage, times(6 + 3)).delete(anyString());
    }

    @Test
    void theScheduledRunDoesNothingWhenOffOrUnconfigured() {
        cleanup(Mode.OFF, NOW).runScheduled();
        when(storage.isConfigured()).thenReturn(false);
        cleanup(Mode.DELETE, NOW).runScheduled();

        verify(storage, never()).readCleanupOwner();
        verify(storage, never()).listObjects();
        verifyNoInteractions(references, tracker);
    }

    @Test
    void theScheduledRunSurvivesAFailureAndDeletesNothing() {
        when(storage.isConfigured()).thenReturn(true);
        when(storage.listObjects()).thenThrow(new IllegalStateException("R2 is down"));

        cleanup(Mode.DELETE, NOW).runScheduled();

        verifyNoInteractions(references);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void settingsThatWouldDeleteTooEagerlyAreRefusedAtStartup() {
        assertThatThrownBy(() -> new StorageCleanup(storage, references, tracker, "delete", 0, 500))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retention-days");
        assertThatThrownBy(() -> new StorageCleanup(storage, references, tracker, "delete", 7, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-deletions-per-run");
        assertThatThrownBy(() -> new StorageCleanup(storage, references, tracker, "true", 7, 500))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("off, dry-run or delete");
        assertThat(Mode.parse(" Dry-Run ")).isEqualTo(Mode.DRY_RUN);
    }

    private void bucketHolds(String... keys) {
        List<StoredObject> objects = new ArrayList<>(List.of(new StoredObject(IN_USE, LONG_AGO)));
        for (String key : keys) {
            objects.add(new StoredObject(key, LONG_AGO));
        }
        when(storage.listObjects()).thenReturn(objects);
    }

    private static String material() {
        return "materials/" + UUID.randomUUID() + ".pdf";
    }

    private StorageCleanup cleanup(Mode mode, Instant now) {
        return new StorageCleanup(storage, references, tracker, Clock.fixed(now, ZoneOffset.UTC), mode, RETENTION, 500);
    }
}
