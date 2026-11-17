package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import pl.edu.agh.backend.storage.StorageOrphanTracker.CleanupState;

@ExtendWith(MockitoExtension.class)
class StorageCleanupTest {

    private static final Instant NOW = Instant.parse("2026-11-16T03:30:00Z");
    private static final Duration RETENTION = Duration.ofDays(7);
    private static final Instant LONG_AGO = NOW.minus(Duration.ofDays(400));
    private static final Instant LAST_NIGHT = NOW.minus(Duration.ofDays(1));
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
    void aBucketThisDatabaseHasBeenCleaning() {
        lenient().when(tracker.state()).thenReturn(new CleanupState(DATABASE_ID, 5, LAST_NIGHT));
        lenient().when(tracker.nextGeneration()).thenReturn(6L);
        lenient().when(storage.readCleanupOwner()).thenReturn(Optional.of(DATABASE_ID + ":5"));
        lenient().when(references.referencedKeys()).thenReturn(Set.of(IN_USE));
        lenient().when(references.isReferenced(anyString())).thenReturn(false);
        lenient().when(tracker.sync(any(), any())).thenAnswer(call -> {
            Set<String> orphans = call.getArgument(0);
            noted.keySet().retainAll(orphans);
            orphans.forEach(key -> noted.putIfAbsent(key, call.getArgument(1)));
            return new HashMap<>(noted);
        });
        lenient()
                .doAnswer(call -> {
                    noted.replaceAll((key, since) -> call.getArgument(0));
                    return null;
                })
                .when(tracker)
                .restartCountdowns(any());
    }

    @Test
    void aFileIsDeletedOnlyAfterStayingOrphanedForTheWholeRetention() {
        String key = material();
        bucketHolds(key);

        // first seen orphaned tonight, however old the upload is
        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isZero();
        ranAt(NOW.plus(Duration.ofDays(6)));
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
        ranAt(NOW.plus(Duration.ofDays(5)));
        cleanup(Mode.DELETE, NOW.plus(Duration.ofDays(6))).removeOrphans();
        when(references.referencedKeys()).thenReturn(Set.of(IN_USE));

        // orphaned again on day 8: the first six days do not count
        ranAt(NOW.plus(Duration.ofDays(7)));
        assertThat(cleanup(Mode.DELETE, NOW.plus(Duration.ofDays(8))).removeOrphans())
                .isZero();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void afterABreakInTheRunsEveryCountdownStartsAgain() {
        String key = material();
        noted.put(key, LONG_AGO);
        bucketHolds(key);
        // off for a month: the file may have been attached and dropped again without anyone noting it
        ranAt(NOW.minus(Duration.ofDays(30)));

        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isZero();

        verify(tracker).restartCountdowns(NOW);
        verify(storage, never()).delete(anyString());
        assertThat(noted).containsEntry(key, NOW);
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
    void everyDeletingRunIsCountedInTheBucketsMarker() {
        bucketHolds();

        cleanup(Mode.DELETE, NOW).removeOrphans();

        verify(storage).writeCleanupOwner(DATABASE_ID + ":6");
        verify(storage, never()).claimCleanupOwner(anyString());
        verify(tracker).markRun(NOW);
    }

    @Test
    void aDryRunTracksAndReportsButWritesNothingToTheBucket() {
        String key = material();
        noted.put(key, LONG_AGO);
        bucketHolds(key);

        assertThat(cleanup(Mode.DRY_RUN, NOW).removeOrphans()).isZero();

        verify(tracker).sync(Set.of(key), NOW);
        verify(tracker, never()).nextGeneration();
        verify(tracker, never()).forget(anyString());
        verify(storage, never()).delete(anyString());
        verify(storage, never()).writeCleanupOwner(anyString());
        verify(storage, never()).claimCleanupOwner(anyString());
    }

    @Test
    void aDryRunDoesNotClaimAnUnclaimedBucket() {
        when(storage.readCleanupOwner()).thenReturn(Optional.empty());
        bucketHolds(material());

        cleanup(Mode.DRY_RUN, NOW).removeOrphans();

        verify(storage, never()).claimCleanupOwner(anyString());
        verify(storage, never()).writeCleanupOwner(anyString());
        verify(tracker, never()).restartCountdowns(any());
    }

    @Test
    void theFirstDeletingRunClaimsTheBucketAndDistrustsOlderNotes() {
        String key = material();
        noted.put(key, LONG_AGO); // noted by a dry run, or by whoever cleaned the bucket before
        bucketHolds(key);
        when(storage.readCleanupOwner()).thenReturn(Optional.empty(), Optional.of(DATABASE_ID + ":6"));

        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isZero();

        verify(storage).claimCleanupOwner(DATABASE_ID + ":6");
        verify(tracker).restartCountdowns(NOW);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void aClaimLostToAnotherDatabaseStopsTheRun() {
        bucketHolds(material());
        when(storage.readCleanupOwner())
                .thenReturn(Optional.empty(), Optional.of("0f3a1c8e-0000-4000-8000-00000000000b:1"));

        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another database claimed");

        verify(tracker, never()).sync(any(), any());
        verify(storage, never()).delete(anyString());
    }

    @Test
    void aDatabaseThatWouldNotBeBelievedDoesNotGetToClaimTheBucket() {
        when(storage.readCleanupOwner()).thenReturn(Optional.empty());
        when(storage.listObjects()).thenReturn(List.of(new StoredObject(material(), LONG_AGO)));

        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("references none");

        verify(storage, never()).claimCleanupOwner(anyString());
        verify(tracker, never()).nextGeneration();
    }

    @Test
    void aBucketCleanedByAnotherDatabaseIsLeftAlone() {
        when(storage.readCleanupOwner()).thenReturn(Optional.of("0f3a1c8e-0000-4000-8000-00000000000b:5"));

        for (Mode mode : new Mode[] {Mode.DELETE, Mode.DRY_RUN}) {
            assertThatThrownBy(() -> cleanup(mode, NOW).removeOrphans())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("two databases");
        }

        verify(storage, never()).listObjects();
        verify(storage, never()).writeCleanupOwner(anyString());
        verifyNoInteractions(references);
        verify(tracker, never()).sync(any(), any());
    }

    @Test
    void aCopyOfTheOwningDatabaseIsLeftBehindByItsRunNumber() {
        // the original went on to run 9 after this copy was taken at run 5
        when(storage.readCleanupOwner()).thenReturn(Optional.of(DATABASE_ID + ":9"));
        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a copy or was restored");

        // and a database that is further along than the bucket knows did not get there by cleaning it
        when(storage.readCleanupOwner()).thenReturn(Optional.of(DATABASE_ID + ":3"));
        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans()).isInstanceOf(IllegalStateException.class);

        verify(storage, never()).listObjects();
        verify(tracker, never()).nextGeneration();
    }

    @Test
    void aRunThatDiedBeforeWritingItsMarkerDoesNotLockTheBucket() {
        when(storage.readCleanupOwner()).thenReturn(Optional.of(DATABASE_ID + ":4"));
        bucketHolds();

        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isZero();

        verify(storage).writeCleanupOwner(DATABASE_ID + ":6");
    }

    @Test
    void anUnreadableMarkerStopsTheRun() {
        when(storage.readCleanupOwner()).thenReturn(Optional.of("not-a-marker"));

        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unreadable");

        verify(storage, never()).listObjects();
    }

    @Test
    void aDatabaseThatKnowsNoneOfTheBucketsFilesIsNotBelieved() {
        String key = material();
        noted.put(key, LONG_AGO);
        when(storage.listObjects()).thenReturn(List.of(new StoredObject(key, LONG_AGO)));
        // not empty: an external link and a file of some other bucket
        when(references.referencedKeys()).thenReturn(Set.of("https://youtu.be/abc", material()));

        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("references none");

        verify(tracker, never()).sync(any(), any());
        verify(storage, never()).writeCleanupOwner(anyString());
        verify(storage, never()).delete(anyString());
    }

    @Test
    void tooLargeAShareOfOrphansIsNotBelievedEither() {
        String[] orphans = new String[30];
        for (int i = 0; i < orphans.length; i++) {
            orphans[i] = material();
        }
        bucketHolds(orphans); // 30 of 31

        assertThatThrownBy(() -> cleanup(Mode.DELETE, NOW).removeOrphans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("more than 50%");
        verify(tracker, never()).sync(any(), any());

        // a small bucket is judged by the other guards only
        bucketHolds(material(), material());
        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isZero();
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
                                2,
                                100)
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
    void aNoteThatCannotBeDroppedIsNotAStorageFailure() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            keys.add(material());
            noted.put(keys.get(i), LONG_AGO);
        }
        bucketHolds(keys.toArray(String[]::new));
        doAnswer(call -> {
                    throw new IllegalStateException("database hiccup");
                })
                .when(tracker)
                .forget(anyString());

        assertThat(cleanup(Mode.DELETE, NOW).removeOrphans()).isEqualTo(5);
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
        assertThatThrownBy(() -> new StorageCleanup(storage, references, tracker, "delete", 0, 500, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retention-days");
        assertThatThrownBy(() -> new StorageCleanup(storage, references, tracker, "delete", 7, 0, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-deletions-per-run");
        assertThatThrownBy(() -> new StorageCleanup(storage, references, tracker, "delete", 7, 500, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-orphan-percent");
        assertThatThrownBy(() -> new StorageCleanup(storage, references, tracker, "true", 7, 500, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("off, dry-run or delete");
        assertThat(Mode.parse(" Dry-Run ")).isEqualTo(Mode.DRY_RUN);
        // set but empty, the way the other R2_* variables may be
        assertThat(Mode.parse("")).isEqualTo(Mode.OFF);
    }

    /** The previous run was at {@code when}, so the next one follows it without a break. */
    private void ranAt(Instant when) {
        when(tracker.state()).thenReturn(new CleanupState(DATABASE_ID, 5, when));
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
        return new StorageCleanup(
                storage, references, tracker, Clock.fixed(now, ZoneOffset.UTC), mode, RETENTION, 500, 50);
    }
}
