package pl.edu.agh.backend.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Object storage (Cloudflare R2) behind a <b>private</b> bucket. The backend never touches file bytes and
 * never exposes a permanent URL:
 *
 * <ul>
 *   <li>uploads: clients PUT straight to R2 through a short-lived presigned URL;
 *   <li>downloads: the database stores the object key, and read endpoints turn it into a presigned GET URL
 *       that stops working after about an hour. Only callers the endpoint already lets see the record get one.
 * </ul>
 *
 * <p>A signed URL is a bearer link: anyone holding it can read that one object until it expires. That is the
 * accepted trade-off for letting clients fetch files from R2 directly instead of through the backend.
 *
 * <p>Built by {@link StorageConfig}. When R2 is not configured the presigner is {@code null}: uploads fail with
 * 503 and keys cannot be resolved, but external {@code http(s)} URLs keep passing through untouched.
 */
public class StorageService implements AutoCloseable {

    /**
     * Objects are private and addressed by random keys that are never reused for other bytes. {@code private}
     * keeps shared caches from storing them; the browser may reuse its copy while the URL is still the same.
     */
    static final String CACHE_CONTROL = "private, max-age=3600";

    /** How long a download URL stays valid. */
    static final Duration DOWNLOAD_URL_TTL = Duration.ofHours(1);

    /**
     * A cached download URL is only reused while at least this much of its lifetime is left, so a client never
     * receives a link that is about to die, yet the URL stays identical for ~50 minutes — which keeps image
     * caches in the apps effective (a fresh signature would change the URL on every request).
     */
    static final Duration MIN_REMAINING_VALIDITY = Duration.ofMinutes(10);

    private static final int MAX_CACHED_URLS = 5_000;

    private static final int MAX_DOWNLOAD_NAME_LENGTH = 150;

    /** Set by the HTTP client itself (and forbidden to set from browser code), so not handed to callers. */
    private static final Set<String> CLIENT_MANAGED_HEADERS = Set.of("host", "content-length");

    private final S3Presigner presigner;
    private final String bucket;
    private final URI endpoint;
    private final Cache<String, String> downloadUrls;

    /**
     * @param presigner {@code null} when storage is not configured
     * @param endpoint S3 API origin of the bucket, e.g. {@code https://<account>.eu.r2.cloudflarestorage.com}
     */
    StorageService(S3Presigner presigner, String bucket, URI endpoint, Clock clock) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.endpoint = endpoint;
        // An entry is dropped MIN_REMAINING_VALIDITY before its URL dies, so a cached URL is always safe to hand out.
        this.downloadUrls = Caffeine.newBuilder()
                .maximumSize(MAX_CACHED_URLS)
                .expireAfterWrite(DOWNLOAD_URL_TTL.minus(MIN_REMAINING_VALIDITY))
                .ticker(() -> clock.millis() * 1_000_000L)
                .build();
    }

    public boolean isConfigured() {
        return presigner != null;
    }

    public PresignedUploadResponse presignAvatar(UUID userId, String contentType, long sizeBytes) {
        UploadCategory category = UploadCategory.AVATAR;
        return presignUpload(category, category.keyPrefix() + "/" + userId, contentType, sizeBytes);
    }

    public PresignedUploadResponse presignAdminUpload(AdminUploadPurpose purpose, String contentType, long sizeBytes) {
        UploadCategory category = purpose.category();
        return presignUpload(category, category.keyPrefix(), contentType, sizeBytes);
    }

    /**
     * Whether {@code key} is an avatar this service issued for {@code userId}. Avatars live under the user's own
     * key folder, so one user cannot point their profile at someone else's file.
     */
    boolean isAvatarKeyOwnedBy(UUID userId, String key) {
        return key.startsWith(UploadCategory.AVATAR.keyPrefix() + "/" + userId + "/")
                && UploadCategory.AVATAR.isValidKey(key);
    }

    /**
     * Turns the {@code avatarKey} a user sent into what gets stored: {@code null} for blank (removes the avatar),
     * otherwise the trimmed key, which must be an avatar this service issued for {@code userId}.
     *
     * @throws InvalidFileException when the key is not the user's own avatar upload
     * @throws StorageNotConfiguredException for a key while storage is not configured
     */
    public String toStoredAvatarKey(UUID userId, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        requireConfigured();
        String key = input.trim();
        if (!isAvatarKeyOwnedBy(userId, key)) {
            throw new InvalidFileException(
                    "avatarKey must be the objectKey of an avatar uploaded through the avatar presign endpoint");
        }
        return key;
    }

    /**
     * Turns a value an admin sent for a file field into what gets stored: {@code null} for blank, an external
     * {@code http(s)} URL unchanged, or an object key of {@code purpose}. A signed download URL of our own bucket
     * (what the read endpoints return, and clients echo back when an edit form is saved unchanged) is reduced to
     * its key, so an expiring link never lands in the database.
     *
     * @throws InvalidFileException for anything else, {@link StorageNotConfiguredException} for a key while storage is not configured
     */
    public String toStoredReference(AdminUploadPurpose purpose, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String value = input.trim();
        UploadCategory category = purpose.category();
        if (isHttpUrl(value)) {
            String ownKey = keyFromOwnUrl(value);
            if (ownKey == null) {
                return value;
            }
            value = ownKey;
        }
        if (!category.isValidKey(value)) {
            throw new InvalidFileException(
                    "Expected an http(s) URL or the objectKey of a file uploaded for " + purpose);
        }
        requireConfigured();
        return value;
    }

    /**
     * What clients get to see for a stored value: an external {@code http(s)} URL as is, an object key as a
     * presigned GET URL, {@code null} for nothing. Anything that is neither (legacy garbage) is returned as it
     * is, which is what the API did before storage existed.
     */
    public String resolveUrl(String stored) {
        return resolveUrl(stored, null);
    }

    /**
     * Like {@link #resolveUrl(String)}, but the signed URL makes the browser save the file as
     * {@code downloadName.<extension of the key>} instead of the random {@code <uuid>.<ext>} it is stored under.
     * Meant for files people download (materials); images shown inline do not need it.
     */
    public String resolveDownloadUrl(String stored, String downloadName) {
        return resolveUrl(stored, downloadName);
    }

    private String resolveUrl(String stored, String downloadName) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (isHttpUrl(stored) || !isKnownKey(stored)) {
            return stored;
        }
        // A key we cannot sign (storage not configured) is not worth handing out as a broken link.
        return isConfigured() ? presignDownload(stored, downloadName) : null;
    }

    private String presignDownload(String key, String downloadName) {
        String disposition = downloadName == null ? null : contentDisposition(downloadName, key);
        // The URL carries the disposition, so one object can have one cached URL per download name.
        String cacheKey = disposition == null ? key : key + '\n' + disposition;
        return downloadUrls.get(cacheKey, ignored -> signDownload(key, disposition));
    }

    private String signDownload(String key, String contentDisposition) {
        GetObjectRequest.Builder object =
                GetObjectRequest.builder().bucket(bucket).key(key);
        if (contentDisposition != null) {
            object.responseContentDisposition(contentDisposition);
        }
        return presigner
                .presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(DOWNLOAD_URL_TTL)
                        .getObjectRequest(object.build())
                        .build())
                .url()
                .toExternalForm();
    }

    /** {@code attachment} header with an ASCII fallback name and the full UTF-8 name (RFC 6266 / 5987). */
    static String contentDisposition(String downloadName, String key) {
        String extension = key.substring(key.lastIndexOf('.') + 1);
        String base = downloadName
                .replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", " ")
                .replaceAll("\\s+", " ")
                .strip();
        if (base.length() > MAX_DOWNLOAD_NAME_LENGTH) {
            base = base.substring(0, MAX_DOWNLOAD_NAME_LENGTH).strip();
        }
        if (base.isEmpty()) {
            base = "download";
        }
        String fileName = base + "." + extension;
        String ascii = Normalizer.normalize(fileName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "_");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''"
                + UriUtils.encode(fileName, StandardCharsets.UTF_8);
    }

    private PresignedUploadResponse presignUpload(
            UploadCategory category, String keyPrefix, String contentType, long sizeBytes) {
        requireConfigured();
        String normalizedType = contentType.trim().toLowerCase(Locale.ROOT);
        String extension = category.extensionFor(normalizedType)
                .orElseThrow(() -> new UnsupportedFileTypeException(normalizedType));
        if (sizeBytes <= 0) {
            throw new InvalidFileException("sizeBytes must be positive");
        }
        if (sizeBytes > category.maxBytes()) {
            throw new FileTooLargeException(category.maxBytes());
        }

        String key = keyPrefix + "/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest putObject = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(normalizedType)
                .contentLength(sizeBytes)
                .cacheControl(CACHE_CONTROL)
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(category.uploadUrlTtl())
                .putObjectRequest(putObject)
                .build());

        return new PresignedUploadResponse(
                presigned.url().toExternalForm(), headersToSend(presigned), key, presigned.expiration());
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new StorageNotConfiguredException();
        }
    }

    private static boolean isKnownKey(String value) {
        for (UploadCategory category : UploadCategory.values()) {
            if (category.isValidKey(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHttpUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /** The object key if {@code url} points into our bucket on our S3 endpoint, otherwise {@code null}. */
    private String keyFromOwnUrl(String url) {
        if (!isConfigured()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String bucketPath = "/" + bucket + "/";
            if (uri.getHost() != null
                    && uri.getHost().equalsIgnoreCase(endpoint.getHost())
                    && uri.getPath() != null
                    && uri.getPath().startsWith(bucketPath)) {
                return uri.getPath().substring(bucketPath.length());
            }
        } catch (URISyntaxException e) {
            return null;
        }
        return null;
    }

    private static Map<String, String> headersToSend(PresignedPutObjectRequest presigned) {
        Map<String, String> headers = new LinkedHashMap<>();
        presigned.signedHeaders().forEach((name, values) -> {
            if (!CLIENT_MANAGED_HEADERS.contains(name.toLowerCase(Locale.ROOT)) && !values.isEmpty()) {
                headers.put(name, values.getFirst());
            }
        });
        return headers;
    }

    @Override
    public void close() {
        if (presigner != null) {
            presigner.close();
        }
    }
}
