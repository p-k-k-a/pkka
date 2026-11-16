package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageCleanupTest {

    private static final Instant NOW = Instant.parse("2026-11-16T03:30:00Z");
    private static final Duration RETENTION = Duration.ofDays(7);
    private static final Instant OLD = NOW.minus(RETENTION).minusSeconds(1);
    private static final Instant FRESH = NOW.minus(RETENTION).plusSeconds(1);

    @Mock
    private StorageService storage;

    @Mock
    private StoredFileReferences references;

    @Test
    void deletesOldFilesNothingPointsAt() {
        String orphanCover = "covers/events/" + UUID.randomUUID() + ".png";
        String orphanAvatar = "avatars/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
        String orphanPostImage = "posts/images/" + UUID.randomUUID() + ".webp";
        when(storage.listObjects())
                .thenReturn(List.of(
                        new StoredObject(orphanCover, OLD),
                        new StoredObject(orphanAvatar, OLD),
                        new StoredObject(orphanPostImage, OLD)));
        when(references.referencedKeys()).thenReturn(Set.of());

        assertThat(cleanup(true, 500).removeOrphans()).isEqualTo(3);

        verify(storage).delete(orphanCover);
        verify(storage).delete(orphanAvatar);
        verify(storage).delete(orphanPostImage);
    }

    @Test
    void keepsWhatIsReferencedFreshOrNotOurs() {
        String referenced = "materials/" + UUID.randomUUID() + ".pdf";
        String justUploaded = "covers/posts/" + UUID.randomUUID() + ".png";
        String someoneElses = "backups/2026-11-01.zip";
        String almostOurs = "materials/notes.pdf";
        when(storage.listObjects())
                .thenReturn(List.of(
                        new StoredObject(referenced, OLD),
                        new StoredObject(justUploaded, FRESH),
                        new StoredObject(someoneElses, OLD),
                        new StoredObject(almostOurs, OLD)));
        when(references.referencedKeys()).thenReturn(Set.of(referenced, "https://youtu.be/abc"));

        assertThat(cleanup(true, 500).removeOrphans()).isZero();

        verify(storage, never()).delete(anyString());
    }

    @Test
    void oneFailedDeletionDoesNotStopTheRest() {
        String first = "materials/" + UUID.randomUUID() + ".pdf";
        String second = "materials/" + UUID.randomUUID() + ".pdf";
        when(storage.listObjects()).thenReturn(List.of(new StoredObject(first, OLD), new StoredObject(second, OLD)));
        when(references.referencedKeys()).thenReturn(Set.of());
        doThrow(new IllegalStateException("R2 said no")).when(storage).delete(first);

        assertThat(cleanup(true, 500).removeOrphans()).isEqualTo(1);

        verify(storage).delete(second);
    }

    @Test
    void aRunDeletesNoMoreThanItsLimit() {
        List<StoredObject> orphans = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            orphans.add(new StoredObject("materials/" + UUID.randomUUID() + ".pdf", OLD));
        }
        when(storage.listObjects()).thenReturn(orphans);
        when(references.referencedKeys()).thenReturn(Set.of());

        assertThat(cleanup(true, 2).removeOrphans()).isEqualTo(2);

        verify(storage).delete(orphans.get(0).key());
        verify(storage).delete(orphans.get(1).key());
        verify(storage, never()).delete(orphans.get(2).key());
    }

    @Test
    void theScheduledRunDoesNothingUnlessEnabledAndConfigured() {
        cleanup(false, 500).runScheduled();
        verifyNoInteractions(references);
        verify(storage, never()).listObjects();

        when(storage.isConfigured()).thenReturn(false);
        cleanup(true, 500).runScheduled();
        verifyNoInteractions(references);
        verify(storage, never()).listObjects();
    }

    @Test
    void aBucketThatCannotBeListedDeletesNothing() {
        when(storage.isConfigured()).thenReturn(true);
        when(storage.listObjects()).thenThrow(new IllegalStateException("R2 is down"));

        cleanup(true, 500).runScheduled();

        verifyNoInteractions(references);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void aDatabaseThatCannotBeReadDeletesNothing() {
        when(storage.isConfigured()).thenReturn(true);
        when(storage.listObjects())
                .thenReturn(List.of(new StoredObject("materials/" + UUID.randomUUID() + ".pdf", OLD)));
        when(references.referencedKeys()).thenThrow(new IllegalStateException("database is down"));

        cleanup(true, 500).runScheduled();

        verify(storage, never()).delete(anyString());
    }

    private StorageCleanup cleanup(boolean enabled, int maxDeletionsPerRun) {
        return new StorageCleanup(
                storage, references, Clock.fixed(NOW, ZoneOffset.UTC), enabled, RETENTION, maxDeletionsPerRun);
    }
}
