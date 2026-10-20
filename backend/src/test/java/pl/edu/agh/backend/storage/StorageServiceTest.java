package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/** Presigning is local signing, so these run without R2, Spring or Docker. */
class StorageServiceTest {

    private static final String ENDPOINT = "https://acct.r2.cloudflarestorage.com";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String COVER_KEY = "covers/events/0f3a1c8e-1111-4000-8000-000000000001.png";
    private static final String MATERIAL_KEY = "materials/0f3a1c8e-1111-4000-8000-000000000002.pdf";

    private S3Presigner presigner;
    private AdjustableClock clock;
    private StorageService storage;

    @BeforeEach
    void setUp() {
        presigner = spy(StorageConfig.presigner(ENDPOINT, "key-id", "secret"));
        clock = new AdjustableClock(Instant.now());
        storage = new StorageService(presigner, "pkka-files", URI.create(ENDPOINT), clock);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    // --- uploads -----------------------------------------------------------------------------------

    @Test
    void avatarUploadUrlIsSignedForExactTypeAndSize() {
        PresignedUploadResponse response = storage.presignAvatar(USER_ID, "image/webp", 123_456);

        URI uploadUrl = URI.create(response.uploadUrl());
        assertThat(uploadUrl.getHost()).isEqualTo("acct.r2.cloudflarestorage.com");
        assertThat(uploadUrl.getPath()).isEqualTo("/pkka-files/" + response.objectKey());
        assertThat(response.objectKey()).matches("avatars/" + USER_ID + "/[0-9a-f-]{36}\\.webp");

        assertThat(uploadUrl.getQuery()).contains("X-Amz-Expires=600");
        assertThat(signedHeaders(uploadUrl)).contains("content-type", "content-length", "cache-control");
        assertThat(response.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void materialUrlsStayValidLongerForLargeFiles() {
        PresignedUploadResponse material = storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "video/mp4", 1024);

        assertThat(URI.create(material.uploadUrl()).getQuery()).contains("X-Amz-Expires=1800");
    }

    @Test
    void uploadUrlCarriesNoChecksumParametersThatR2OrBrowsersCannotSatisfy() {
        String url = storage.presignAvatar(USER_ID, "image/png", 1024).uploadUrl();

        assertThat(url.toLowerCase(Locale.ROOT)).doesNotContain("checksum");
    }

    @Test
    void returnsOnlyHeadersTheClientMustSetItself() {
        PresignedUploadResponse response = storage.presignAvatar(USER_ID, "image/png", 1024);

        assertThat(response.headers())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("content-type", "image/png", "cache-control", StorageService.CACHE_CONTROL));
    }

    @Test
    void storedObjectsAreNotCacheableBySharedCaches() {
        assertThat(StorageService.CACHE_CONTROL).startsWith("private");
    }

    @Test
    void normalisesContentType() {
        PresignedUploadResponse response = storage.presignAvatar(USER_ID, "  IMAGE/JPEG ", 1024);

        assertThat(response.headers()).containsEntry("content-type", "image/jpeg");
        assertThat(response.objectKey()).endsWith(".jpg");
    }

    @Test
    void everyUploadGetsItsOwnKey() {
        String first = storage.presignAvatar(USER_ID, "image/png", 1024).objectKey();
        String second = storage.presignAvatar(USER_ID, "image/png", 1024).objectKey();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsContentTypesOutsideTheWhitelist() {
        assertStatus(() -> storage.presignAvatar(USER_ID, "image/svg+xml", 1024), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertStatus(() -> storage.presignAvatar(USER_ID, "application/pdf", 1024), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertStatus(
                () -> storage.presignAvatar(USER_ID, "image/png; charset=x", 1024), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "text/html", 1024),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "application/pdf", 1024),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void enforcesPerPurposeSizeLimits() {
        long mb = 1024L * 1024L;

        storage.presignAvatar(USER_ID, "image/png", 2 * mb);
        assertStatus(() -> storage.presignAvatar(USER_ID, "image/png", 2 * mb + 1), HttpStatus.CONTENT_TOO_LARGE);

        storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "image/png", 5 * mb);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "image/png", 5 * mb + 1),
                HttpStatus.CONTENT_TOO_LARGE);

        storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "video/mp4", 100 * mb);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "video/mp4", 100 * mb + 1),
                HttpStatus.CONTENT_TOO_LARGE);
    }

    @Test
    void rejectsEmptyFiles() {
        assertStatus(() -> storage.presignAvatar(USER_ID, "image/png", 0), HttpStatus.BAD_REQUEST);
    }

    @Test
    void adminUploadsLandUnderTheirPurposePrefix() {
        PresignedUploadResponse material =
                storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "application/pdf", 10);
        PresignedUploadResponse cover = storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "image/jpeg", 10);

        assertThat(material.objectKey()).matches("materials/[0-9a-f-]{36}\\.pdf");
        assertThat(cover.objectKey()).matches("covers/events/[0-9a-f-]{36}\\.jpg");
    }

    @Test
    void issuedKeysAreAcceptedByTheirOwnCategory() {
        String material = storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "video/webm", 10)
                .objectKey();
        String cover = storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "image/webp", 10)
                .objectKey();

        assertThat(storage.toStoredReference(AdminUploadPurpose.MATERIAL, material))
                .isEqualTo(material);
        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, cover))
                .isEqualTo(cover);
    }

    @Test
    void answers503WhenStorageIsNotConfigured() {
        StorageService unconfigured = unconfigured();

        assertThat(unconfigured.isConfigured()).isFalse();
        assertStatus(() -> unconfigured.presignAvatar(USER_ID, "image/png", 1024), HttpStatus.SERVICE_UNAVAILABLE);
        assertStatus(
                () -> unconfigured.presignAdminUpload(AdminUploadPurpose.MATERIAL, "application/pdf", 1024),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    // --- avatar ownership --------------------------------------------------------------------------

    @Test
    void recognisesOwnAvatarKey() {
        String key = storage.presignAvatar(USER_ID, "image/png", 1024).objectKey();

        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, key)).isTrue();
    }

    @Test
    void rejectsAvatarKeysThatAreNotTheUsersOwnUpload() {
        UUID otherUser = UUID.randomUUID();
        String own = storage.presignAvatar(USER_ID, "image/png", 1024).objectKey();
        String file = own.substring(own.lastIndexOf('/') + 1);
        String ownFolder = "avatars/" + USER_ID + "/";

        assertThat(storage.isAvatarKeyOwnedBy(otherUser, own)).isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, "https://evil.example.com/" + own))
                .isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, own + "?x=1")).isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, ownFolder + "../" + file))
                .isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, ownFolder + "nested/" + file))
                .isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, ownFolder + UUID.randomUUID() + ".svg"))
                .isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, own + " ")).isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, own.replace(".png", ".PNG")))
                .isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, own.replace(file, file.toUpperCase(Locale.ROOT))))
                .isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, ownFolder + "%2e%2e/" + file))
                .isFalse();
        assertThat(storage.isAvatarKeyOwnedBy(USER_ID, "materials/" + file)).isFalse();
    }

    @Test
    void noAvatarIsOwnedWhenStorageIsNotConfigured() {
        assertThat(unconfigured().isAvatarKeyOwnedBy(USER_ID, "avatars/" + USER_ID + "/x.png"))
                .isFalse();
    }

    // --- stored references (admin input) -----------------------------------------------------------

    @Test
    void blankReferencesAreStoredAsNull() {
        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, null))
                .isNull();
        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, "   "))
                .isNull();
    }

    @Test
    void externalUrlsAreKeptAsTheyAre() {
        assertThat(storage.toStoredReference(AdminUploadPurpose.MATERIAL, " https://youtu.be/abc "))
                .isEqualTo("https://youtu.be/abc");
        assertThat(unconfigured().toStoredReference(AdminUploadPurpose.MATERIAL, "https://youtu.be/abc"))
                .isEqualTo("https://youtu.be/abc");
    }

    @Test
    void aSignedUrlOfOurBucketIsReducedToItsKey() {
        String signed = storage.resolveUrl(COVER_KEY);

        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, signed))
                .isEqualTo(COVER_KEY);
    }

    @Test
    void aKeyForAnotherPurposeIsRejected() {
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, MATERIAL_KEY), HttpStatus.BAD_REQUEST);
        assertStatus(() -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, COVER_KEY), HttpStatus.BAD_REQUEST);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, storage.resolveUrl(COVER_KEY)),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void valuesThatAreNeitherUrlNorOurKeyAreRejected() {
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, "materials/x.pdf"),
                HttpStatus.BAD_REQUEST);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, "materials/../" + MATERIAL_KEY),
                HttpStatus.BAD_REQUEST);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, "meet.google.com/x"),
                HttpStatus.BAD_REQUEST);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, MATERIAL_KEY.toUpperCase(Locale.ROOT)),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void aKeyCannotBeStoredWhileStorageIsNotConfigured() {
        assertStatus(
                () -> unconfigured().toStoredReference(AdminUploadPurpose.MATERIAL, MATERIAL_KEY),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    // --- downloads ---------------------------------------------------------------------------------

    @Test
    void anObjectKeyBecomesASignedGetUrlThatExpiresInAnHour() {
        URI url = URI.create(storage.resolveUrl(MATERIAL_KEY));

        assertThat(url.getHost()).isEqualTo("acct.r2.cloudflarestorage.com");
        assertThat(url.getPath()).isEqualTo("/pkka-files/" + MATERIAL_KEY);
        assertThat(url.getQuery()).contains("X-Amz-Expires=3600").contains("X-Amz-Signature=");
        assertThat(url.getQuery().toLowerCase(Locale.ROOT)).doesNotContain("checksum");
    }

    @Test
    void externalAndLegacyValuesPassThroughAndNothingStaysNull() {
        assertThat(storage.resolveUrl(null)).isNull();
        assertThat(storage.resolveUrl("  ")).isNull();
        assertThat(storage.resolveUrl("https://cdn.example.com/a.png")).isEqualTo("https://cdn.example.com/a.png");
        assertThat(storage.resolveUrl("HTTP://cdn.example.com/a.png")).isEqualTo("HTTP://cdn.example.com/a.png");
        assertThat(storage.resolveUrl("meet.google.com/x")).isEqualTo("meet.google.com/x");
    }

    @Test
    void aKeyIsNotHandedOutAsABrokenLinkWhenStorageIsNotConfigured() {
        StorageService unconfigured = unconfigured();

        assertThat(unconfigured.resolveUrl(MATERIAL_KEY)).isNull();
        assertThat(unconfigured.resolveUrl("https://cdn.example.com/a.png")).isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void theSameUrlIsHandedOutWhileItHasPlentyOfLifeLeft() {
        String first = storage.resolveUrl(MATERIAL_KEY);
        clock.advance(Duration.ofMinutes(45));
        String second = storage.resolveUrl(MATERIAL_KEY);

        assertThat(second).isEqualTo(first);
        verify(presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void aNewUrlIsSignedOnceTheOldOneIsAboutToExpire() {
        storage.resolveUrl(MATERIAL_KEY);
        clock.advance(Duration.ofMinutes(55));
        storage.resolveUrl(MATERIAL_KEY);

        verify(presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void differentKeysGetDifferentUrls() {
        assertThat(storage.resolveUrl(MATERIAL_KEY)).isNotEqualTo(storage.resolveUrl(COVER_KEY));
    }

    // --- helpers -----------------------------------------------------------------------------------

    private StorageService unconfigured() {
        return new StorageService(null, "", URI.create("https://unconfigured.invalid"), clock);
    }

    private static String signedHeaders(URI uploadUrl) {
        String signedHeaders =
                UriComponentsBuilder.fromUri(uploadUrl).build().getQueryParams().getFirst("X-Amz-SignedHeaders");
        assertThat(signedHeaders).isNotNull();
        return UriUtils.decode(signedHeaders, StandardCharsets.UTF_8);
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(expected));
    }

    /** The service only reads the time to decide whether a cached URL is still worth handing out. */
    private static final class AdjustableClock extends Clock {
        private Instant now;

        AdjustableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
