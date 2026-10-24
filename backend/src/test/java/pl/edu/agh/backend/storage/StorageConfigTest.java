package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorageConfigTest {

    private final StorageConfig config = new StorageConfig();

    @Test
    void storageIsDisabledWhenNothingIsConfigured() {
        try (StorageService storage = config.storageService("", "", "", "")) {
            assertThat(storage.isConfigured()).isFalse();
        }
    }

    @Test
    void failsStartupWhenOnlyPartiallyConfigured() {
        assertThatThrownBy(() -> config.storageService("https://acct.r2.cloudflarestorage.com", "bucket", "key", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_SECRET_ACCESS_KEY")
                .hasMessageNotContaining("R2_BUCKET");
    }

    @Test
    void rejectsAnEndpointThatIsNotAnAbsoluteHttpUrl() {
        assertThatThrownBy(() -> config.storageService("acct.r2.cloudflarestorage.com", "b", "k", "s"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_ENDPOINT");
    }

    @Test
    void aFullyConfiguredStorageSignsUploadsAndDownloads() {
        try (StorageService storage =
                config.storageService("https://acct.r2.cloudflarestorage.com", "bucket", "key", "secret")) {
            PresignedUploadResponse response = storage.presignAvatar(UUID.randomUUID(), "image/png", 10);

            assertThat(storage.isConfigured()).isTrue();
            assertThat(response.uploadUrl()).startsWith("https://acct.r2.cloudflarestorage.com/bucket/avatars/");
            assertThat(storage.resolveUrl(response.objectKey()))
                    .startsWith("https://acct.r2.cloudflarestorage.com/bucket/avatars/");
        }
    }
}
