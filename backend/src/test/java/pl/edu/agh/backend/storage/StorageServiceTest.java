package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.DeleteObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/** Presigning is local signing, so these run without R2, Spring or Docker. */
class StorageServiceTest {

    private static final String ENDPOINT = "https://acct.eu.r2.cloudflarestorage.com";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String COVER_KEY = "covers/events/0f3a1c8e-1111-4000-8000-000000000001.png";
    private static final String MATERIAL_KEY = "materials/0f3a1c8e-1111-4000-8000-000000000002.pdf";
    private static final String POST_COVER_KEY = "covers/posts/0f3a1c8e-1111-4000-8000-000000000003.webp";
    private static final String POST_IMAGE_KEY = "posts/images/0f3a1c8e-1111-4000-8000-000000000004.jpg";

    private S3Presigner presigner;
    private S3Client client;
    private AdjustableClock clock;
    private StorageService storage;

    @BeforeEach
    void setUp() {
        presigner = spy(StorageConfig.presigner(ENDPOINT, "key-id", "secret"));
        clock = new AdjustableClock(Instant.now());
        client = mock(S3Client.class);
        // by default every file has been uploaded as it was issued
        lenient().when(client.headObject(any(HeadObjectRequest.class))).thenAnswer(call -> {
            String key = call.<HeadObjectRequest>getArgument(0).key();
            return uploaded(1024, contentTypeOf(key));
        });
        storage = new StorageService(presigner, client, "pkka-files", URI.create(ENDPOINT), clock);
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
        assertThat(uploadUrl.getHost()).isEqualTo("acct.eu.r2.cloudflarestorage.com");
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
        assertStatus(() -> storage.presignAvatar(USER_ID, "image/svg+xml", 1024), UnsupportedFileTypeException.class);
        assertStatus(() -> storage.presignAvatar(USER_ID, "application/pdf", 1024), UnsupportedFileTypeException.class);
        assertStatus(
                () -> storage.presignAvatar(USER_ID, "image/png; charset=x", 1024), UnsupportedFileTypeException.class);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "text/html", 1024),
                UnsupportedFileTypeException.class);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "application/pdf", 1024),
                UnsupportedFileTypeException.class);
    }

    @Test
    void enforcesPerPurposeSizeLimits() {
        long mb = 1024L * 1024L;

        storage.presignAvatar(USER_ID, "image/png", 2 * mb);
        assertStatus(() -> storage.presignAvatar(USER_ID, "image/png", 2 * mb + 1), FileTooLargeException.class);

        storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "image/png", 5 * mb);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "image/png", 5 * mb + 1),
                FileTooLargeException.class);

        storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "video/mp4", 100 * mb);
        assertStatus(
                () -> storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "video/mp4", 100 * mb + 1),
                FileTooLargeException.class);
    }

    @Test
    void rejectsEmptyFiles() {
        assertStatus(() -> storage.presignAvatar(USER_ID, "image/png", 0), InvalidFileException.class);
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
    void postUploadsAreImagesUnderTheirOwnPrefixes() {
        PresignedUploadResponse cover = storage.presignAdminUpload(AdminUploadPurpose.POST_COVER, "image/webp", 10);
        PresignedUploadResponse image = storage.presignAdminUpload(AdminUploadPurpose.POST_IMAGE, "image/png", 10);

        assertThat(cover.objectKey()).matches("covers/posts/[0-9a-f-]{36}\\.webp");
        assertThat(image.objectKey()).matches("posts/images/[0-9a-f-]{36}\\.png");
        assertThat(storage.toStoredReference(AdminUploadPurpose.POST_COVER, cover.objectKey(), null))
                .isEqualTo(cover.objectKey());

        long mb = 1024L * 1024L;
        for (AdminUploadPurpose purpose :
                new AdminUploadPurpose[] {AdminUploadPurpose.POST_COVER, AdminUploadPurpose.POST_IMAGE}) {
            storage.presignAdminUpload(purpose, "image/jpeg", 5 * mb);
            assertStatus(
                    () -> storage.presignAdminUpload(purpose, "image/jpeg", 5 * mb + 1), FileTooLargeException.class);
            assertStatus(
                    () -> storage.presignAdminUpload(purpose, "application/pdf", 10),
                    UnsupportedFileTypeException.class);
        }
    }

    @Test
    void aPostCoverIsNotInterchangeableWithOtherUploads() {
        assertThat(storage.toStoredReference(AdminUploadPurpose.POST_COVER, storage.resolveUrl(POST_COVER_KEY), null))
                .isEqualTo(POST_COVER_KEY);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.POST_COVER, COVER_KEY, null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.POST_COVER, POST_IMAGE_KEY, null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, POST_COVER_KEY, null),
                InvalidFileException.class);
    }

    // --- post content ------------------------------------------------------------------------------

    @Test
    void picturesInPostContentAreServedThroughSignedUrls() {
        String stored = "Wstęp\n\n![Sala](" + POST_IMAGE_KEY + ")\n\n[regulamin](https://example.com/r.pdf)";

        String resolved = storage.resolveContent(stored);

        String signed = storage.resolveUrl(POST_IMAGE_KEY);
        assertThat(signed).startsWith(ENDPOINT + "/pkka-files/" + POST_IMAGE_KEY + "?");
        assertThat(resolved).isEqualTo("Wstęp\n\n![Sala](" + signed + ")\n\n[regulamin](https://example.com/r.pdf)");
    }

    @Test
    void savingEditedContentTurnsSignedUrlsBackIntoKeys() {
        String stored = "![a](" + POST_IMAGE_KEY + ") tekst ![b](" + POST_IMAGE_KEY + " \"podpis\")";

        String echoedBack = storage.resolveContent(stored);

        assertThat(echoedBack).doesNotContain("](" + POST_IMAGE_KEY);
        assertThat(storage.toStoredContent(echoedBack, null)).isEqualTo(stored);
    }

    @Test
    void aFreshlyUploadedPictureIsStoredByItsKey() {
        String content = "![nowe](" + POST_IMAGE_KEY + ")";

        assertThat(storage.toStoredContent(content, null)).isEqualTo(content);
    }

    @Test
    void onlyPostPicturesAreSignedInsideContent() {
        String content = "![x](" + MATERIAL_KEY + ") ![y](" + POST_COVER_KEY + ") " + POST_IMAGE_KEY
                + " ![z](https://example.com/z.png)";

        assertThat(storage.resolveContent(content)).isEqualTo(content);
    }

    @Test
    void linksToOtherSitesAreLeftAloneWhenStoringContent() {
        String foreign = "https://elsewhere.example.com/pkka-files/" + POST_IMAGE_KEY + "?X-Amz-Signature=abc";
        String content = "[strona](https://example.com/a_(b)) ![f](" + foreign + ")";

        assertThat(storage.toStoredContent(content, null)).isEqualTo(content);
    }

    @Test
    void aSignedUrlOfAnotherStoredFileIsRejectedInContent() {
        // it would be saved verbatim and stop working within the hour
        assertStatus(
                () -> storage.toStoredContent("[slajdy](" + storage.resolveUrl(MATERIAL_KEY) + ")", null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredContent("![okładka](" + storage.resolveUrl(COVER_KEY) + ")", null),
                InvalidFileException.class);
    }

    @Test
    void aSignedPostPictureIsReducedToItsKeyEvenInsideAPlainLink() {
        String signed = storage.resolveUrl(POST_IMAGE_KEY);

        assertThat(storage.toStoredContent("[zobacz zdjęcie](" + signed + ")", null))
                .isEqualTo("[zobacz zdjęcie](" + POST_IMAGE_KEY + ")");
    }

    @Test
    void aBrowserLocalPreviewIsRejectedInContent() {
        String content = "![](blob:https://klub.example.com/0f3a1c8e-1111-4000-8000-000000000009)";

        assertStatus(() -> storage.toStoredContent(content, null), InvalidFileException.class);
        assertStatus(() -> unconfigured().toStoredContent(content, null), InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredContent(content.replace("blob:", "BLOB:"), null), InvalidFileException.class);
    }

    @Test
    void anEditFormGetsTheKeyBackWhenItCannotBeSigned() {
        assertThat(storage.resolveForEditing(COVER_KEY)).startsWith(ENDPOINT + "/pkka-files/" + COVER_KEY + "?");
        assertThat(unconfigured().resolveForEditing(COVER_KEY)).isEqualTo(COVER_KEY);
        assertThat(unconfigured().resolveForEditing("https://example.com/c.png"))
                .isEqualTo("https://example.com/c.png");
        assertThat(unconfigured().resolveForEditing(null)).isNull();
        assertThat(unconfigured().resolveForEditing(" ")).isNull();
    }

    @Test
    void contentIsLeftAsItIsWithoutStorageOrWithoutContent() {
        String content = "![a](" + POST_IMAGE_KEY + ")";

        assertThat(unconfigured().resolveContent(content)).isEqualTo(content);
        assertThat(unconfigured().toStoredContent(storage.resolveContent(content), null))
                .isEqualTo(storage.resolveContent(content));
        assertThat(storage.resolveContent(null)).isNull();
        assertThat(storage.toStoredContent(null, null)).isNull();
    }

    @Test
    void issuedKeysAreAcceptedByTheirOwnCategory() {
        String material = storage.presignAdminUpload(AdminUploadPurpose.MATERIAL, "video/webm", 10)
                .objectKey();
        String cover = storage.presignAdminUpload(AdminUploadPurpose.EVENT_COVER, "image/webp", 10)
                .objectKey();

        assertThat(storage.toStoredReference(AdminUploadPurpose.MATERIAL, material, null))
                .isEqualTo(material);
        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, cover, null))
                .isEqualTo(cover);
    }

    @Test
    void answers503WhenStorageIsNotConfigured() {
        StorageService unconfigured = unconfigured();

        assertThat(unconfigured.isConfigured()).isFalse();
        assertStatus(() -> unconfigured.presignAvatar(USER_ID, "image/png", 1024), StorageNotConfiguredException.class);
        assertStatus(
                () -> unconfigured.presignAdminUpload(AdminUploadPurpose.MATERIAL, "application/pdf", 1024),
                StorageNotConfiguredException.class);
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
    void anAvatarKeyCannotBeStoredWhileStorageIsNotConfigured() {
        assertStatus(
                () -> unconfigured().toStoredAvatarKey(USER_ID, "avatars/" + USER_ID + "/x.png", null),
                StorageNotConfiguredException.class);
    }

    @Test
    void anAvatarKeyIsStoredTrimmedAndBlankRemovesIt() {
        String key = storage.presignAvatar(USER_ID, "image/png", 1024).objectKey();

        assertThat(storage.toStoredAvatarKey(USER_ID, "  " + key + " ", null)).isEqualTo(key);
        assertThat(storage.toStoredAvatarKey(USER_ID, "  ", null)).isNull();
        assertThat(unconfigured().toStoredAvatarKey(USER_ID, "", null)).isNull();
        assertStatus(
                () -> storage.toStoredAvatarKey(USER_ID, "avatars/" + UUID.randomUUID() + "/a.png", null),
                InvalidFileException.class);
    }

    @Test
    void aDownloadNameMakesTheBrowserSaveTheFileUnderThatName() {
        URI url = URI.create(storage.resolveDownloadUrl(MATERIAL_KEY, "Prezentacja: Łódź/AI \"2026\""));

        String query = UriUtils.decode(url.getRawQuery(), StandardCharsets.UTF_8);
        assertThat(query)
                .contains("response-content-disposition=attachment; filename=\"Prezentacja _odz AI 2026.pdf\"")
                .contains("filename*=UTF-8''Prezentacja%20%C5%81%C3%B3d%C5%BA%20AI%202026.pdf");
        assertThat(url.getPath()).isEqualTo("/pkka-files/" + MATERIAL_KEY);
    }

    @Test
    void aBlankDownloadNameFallsBackAndExternalUrlsAreUntouched() {
        assertThat(StorageService.contentDisposition(" / ", MATERIAL_KEY)).contains("filename=\"download.pdf\"");
        assertThat(storage.resolveDownloadUrl("https://youtu.be/abc", "Title")).isEqualTo("https://youtu.be/abc");
        assertThat(unconfigured().resolveDownloadUrl(MATERIAL_KEY, "Title")).isNull();
    }

    @Test
    void downloadUrlsAreCachedPerDownloadName() {
        String first = storage.resolveDownloadUrl(MATERIAL_KEY, "A");

        assertThat(storage.resolveDownloadUrl(MATERIAL_KEY, "A")).isEqualTo(first);
        assertThat(storage.resolveDownloadUrl(MATERIAL_KEY, "B")).isNotEqualTo(first);
        assertThat(storage.resolveUrl(MATERIAL_KEY)).doesNotContain("response-content-disposition");
    }

    // --- stored references (admin input) -----------------------------------------------------------

    @Test
    void blankReferencesAreStoredAsNull() {
        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, null, null))
                .isNull();
        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, "   ", null))
                .isNull();
    }

    @Test
    void externalUrlsAreKeptAsTheyAre() {
        assertThat(storage.toStoredReference(AdminUploadPurpose.MATERIAL, " https://youtu.be/abc ", null))
                .isEqualTo("https://youtu.be/abc");
        assertThat(unconfigured().toStoredReference(AdminUploadPurpose.MATERIAL, "https://youtu.be/abc", null))
                .isEqualTo("https://youtu.be/abc");
    }

    @Test
    void aSignedUrlOfOurBucketIsReducedToItsKey() {
        String signed = storage.resolveUrl(COVER_KEY);

        assertThat(storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, signed, null))
                .isEqualTo(COVER_KEY);
    }

    @Test
    void aUrlOfOurBucketIsRecognisedWithoutItsSignature() {
        // the web app sends an uploaded material back without the query, which can get very long
        String unsigned = ENDPOINT + "/pkka-files/" + MATERIAL_KEY;

        assertThat(storage.toStoredReference(AdminUploadPurpose.MATERIAL, unsigned, null))
                .isEqualTo(MATERIAL_KEY);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, unsigned, null),
                InvalidFileException.class);
    }

    @Test
    void aKeyForAnotherPurposeIsRejected() {
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, MATERIAL_KEY, null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, COVER_KEY, null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, storage.resolveUrl(COVER_KEY), null),
                InvalidFileException.class);
    }

    @Test
    void valuesThatAreNeitherUrlNorOurKeyAreRejected() {
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, "materials/x.pdf", null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, "materials/../" + MATERIAL_KEY, null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, "meet.google.com/x", null),
                InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(
                        AdminUploadPurpose.MATERIAL, MATERIAL_KEY.toUpperCase(Locale.ROOT), null),
                InvalidFileException.class);
    }

    @Test
    void aKeyCannotBeStoredWhileStorageIsNotConfigured() {
        assertStatus(
                () -> unconfigured().toStoredReference(AdminUploadPurpose.MATERIAL, MATERIAL_KEY, null),
                StorageNotConfiguredException.class);
        assertStatus(
                () -> unconfigured().toStoredReference(AdminUploadPurpose.MATERIAL, MATERIAL_KEY, "https://youtu.be/a"),
                StorageNotConfiguredException.class);
    }

    @Test
    void aFileTheRecordAlreadyHoldsIsKeptWhileStorageIsNotConfigured() {
        String avatar = "avatars/" + USER_ID + "/" + UUID.randomUUID() + ".png";

        assertThat(unconfigured().toStoredReference(AdminUploadPurpose.MATERIAL, MATERIAL_KEY, MATERIAL_KEY))
                .isEqualTo(MATERIAL_KEY);
        assertThat(unconfigured().toStoredAvatarKey(USER_ID, avatar, avatar)).isEqualTo(avatar);
    }

    // --- downloads ---------------------------------------------------------------------------------

    @Test
    void anObjectKeyBecomesASignedGetUrlThatExpiresInAnHour() {
        URI url = URI.create(storage.resolveUrl(MATERIAL_KEY));

        assertThat(url.getHost()).isEqualTo("acct.eu.r2.cloudflarestorage.com");
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

    // --- deletes -----------------------------------------------------------------------------------

    @Test
    void deleteUrlIsShortLivedAndSignedForTheObject() {
        String key = "avatars/" + USER_ID + "/0f3a1c8e-1111-4000-8000-000000000003.png";

        URI url = URI.create(storage.presignDelete(key));

        assertThat(url.getPath()).isEqualTo("/pkka-files/" + key);
        assertThat(url.getQuery()).contains("X-Amz-Expires=60").contains("X-Amz-Signature=");
    }

    @Test
    void deleteSkipsWhatIsNotOurs() {
        storage.delete(null);
        storage.delete("https://example.com/a.png");

        verify(presigner, times(0)).presignDeleteObject(any(DeleteObjectPresignRequest.class));
    }

    @Test
    void whatTheCleanupNeedsFromTheBucketNeedsAClient() {
        assertStatus(() -> unconfigured().listObjects(), StorageNotConfiguredException.class);
        assertStatus(() -> unconfigured().readCleanupOwner(), StorageNotConfiguredException.class);
        assertStatus(() -> unconfigured().writeCleanupOwner("id"), StorageNotConfiguredException.class);
    }

    @Test
    void theCleanupOwnerMarkerIsNeverTakenForAnUpload() {
        assertThat(StorageService.isIssuedKey(StorageService.CLEANUP_OWNER_KEY)).isFalse();
    }

    @Test
    void onlyKeysOfOurOwnShapeCountAsIssued() {
        assertThat(StorageService.isIssuedKey(MATERIAL_KEY)).isTrue();
        assertThat(StorageService.isIssuedKey(POST_IMAGE_KEY)).isTrue();
        assertThat(StorageService.isIssuedKey("avatars/" + USER_ID + "/0f3a1c8e-1111-4000-8000-000000000005.png"))
                .isTrue();
        assertThat(StorageService.isIssuedKey("materials/notes.pdf")).isFalse();
        assertThat(StorageService.isIssuedKey("backups/" + MATERIAL_KEY)).isFalse();
    }

    // --- files that were never uploaded ------------------------------------------------------------

    @Test
    void aKeyNothingWasUploadedUnderIsNotStored() {
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().statusCode(404).build());
        String avatar = storage.presignAvatar(USER_ID, "image/png", 1024).objectKey();

        assertStatus(() -> storage.toStoredAvatarKey(USER_ID, avatar, null), InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredReference(AdminUploadPurpose.MATERIAL, MATERIAL_KEY, null),
                InvalidFileException.class);
        assertStatus(() -> storage.toStoredContent("![a](" + POST_IMAGE_KEY + ")", null), InvalidFileException.class);
        assertStatus(
                () -> storage.toStoredContent("![a](" + storage.resolveUrl(POST_IMAGE_KEY) + ")", "other content"),
                InvalidFileException.class);
    }

    @Test
    void aFileTheRecordAlreadyPointsAtIsNotLookedUpAgain() {
        String avatar = storage.presignAvatar(USER_ID, "image/png", 1024).objectKey();
        String content = "![a](" + POST_IMAGE_KEY + ")";

        assertThat(storage.toStoredAvatarKey(USER_ID, avatar, avatar)).isEqualTo(avatar);
        assertThat(storage.toStoredReference(
                        AdminUploadPurpose.MATERIAL, storage.resolveUrl(MATERIAL_KEY), MATERIAL_KEY))
                .isEqualTo(MATERIAL_KEY);
        assertThat(storage.toStoredContent(content + " edited", content)).isEqualTo(content + " edited");
        assertThat(storage.toStoredReference(AdminUploadPurpose.MATERIAL, "https://youtu.be/abc", MATERIAL_KEY))
                .isEqualTo("https://youtu.be/abc");

        verify(client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void aPictureUsedTwiceIsLookedUpOnce() {
        storage.toStoredContent("![a](" + POST_IMAGE_KEY + ") ![b](" + POST_IMAGE_KEY + ")", null);

        verify(client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void aFileOfAnotherSizeOrTypeThanAllowedIsNotStored() {
        for (HeadObjectResponse object : List.of(
                uploaded(0, "image/png"),
                uploaded(6L * 1024 * 1024, "image/png"),
                uploaded(1024, "image/jpeg"),
                uploaded(1024, "text/html"),
                HeadObjectResponse.builder().build())) {
            doReturn(object).when(client).headObject(any(HeadObjectRequest.class));
            assertStatus(
                    () -> storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, COVER_KEY, null),
                    InvalidFileException.class);
        }
    }

    @Test
    void storageThatDoesNotAnswerIsNotMistakenForAMissingFile() {
        for (Exception failure : List.of(
                S3Exception.builder().statusCode(403).build(),
                S3Exception.builder().statusCode(500).build(),
                SdkClientException.create("timed out"))) {
            doThrow(failure).when(client).headObject(any(HeadObjectRequest.class));
            assertStatus(
                    () -> storage.toStoredReference(AdminUploadPurpose.EVENT_COVER, COVER_KEY, null),
                    StorageUnavailableException.class);
        }
    }

    private static HeadObjectResponse uploaded(long size, String contentType) {
        return HeadObjectResponse.builder()
                .contentLength(size)
                .contentType(contentType)
                .build();
    }

    private static String contentTypeOf(String key) {
        return switch (key.substring(key.lastIndexOf('.') + 1)) {
            case "pdf" -> "application/pdf";
            case "webm" -> "video/webm";
            case "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
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

    private static void assertStatus(Runnable call, Class<? extends RuntimeException> expected) {
        assertThatThrownBy(call::run).isInstanceOf(expected);
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
