package pl.edu.agh.backend.storage;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * What a file is for. Decides the object key shape, the accepted content types and the size limit.
 * The content type is signed into the presigned URL, so what is accepted here is what R2 will store.
 * Deliberately no HTML or SVG, although the bucket is private and files are only served through
 * short-lived signed URLs.
 *
 * <p>Keys are {@code <prefix>/<uuid>.<ext>}; avatars additionally sit in a per-user folder,
 * {@code avatars/<userId>/<uuid>.<ext>}, which is what ties an avatar to its owner.
 */
enum UploadCategory {
    AVATAR("avatars", 2 * Limits.MB, Duration.ofMinutes(10)),
    EVENT_COVER("covers/events", 5 * Limits.MB, Duration.ofMinutes(10)),
    // Room to upload 100 MB over a slow connection.
    MATERIAL("materials", 100 * Limits.MB, Duration.ofMinutes(30));

    private final String keyPrefix;
    private final long maxBytes;
    private final Duration uploadUrlTtl;

    UploadCategory(String keyPrefix, long maxBytes, Duration uploadUrlTtl) {
        this.keyPrefix = keyPrefix;
        this.maxBytes = maxBytes;
        this.uploadUrlTtl = uploadUrlTtl;
    }

    Duration uploadUrlTtl() {
        return uploadUrlTtl;
    }

    String keyPrefix() {
        return keyPrefix;
    }

    long maxBytes() {
        return maxBytes;
    }

    /** File extension for an accepted content type (already lower-cased), empty when the type is not accepted. */
    Optional<String> extensionFor(String contentType) {
        return Optional.ofNullable(types().get(contentType));
    }

    /** Whether {@code key} has exactly the shape this category issues; anything else is not ours. */
    boolean isValidKey(String key) {
        return KeyPatterns.of(this).matcher(key).matches();
    }

    private Map<String, String> types() {
        return switch (this) {
            case AVATAR, EVENT_COVER -> Types.IMAGES;
            case MATERIAL -> Types.MATERIALS;
        };
    }

    private static final class Limits {
        static final long MB = 1024L * 1024L;
    }

    private static final class KeyPatterns {
        private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

        static Pattern of(UploadCategory category) {
            return switch (category) {
                case AVATAR -> AVATAR;
                case EVENT_COVER -> EVENT_COVER;
                case MATERIAL -> MATERIAL;
            };
        }

        private static final Pattern AVATAR = build(UploadCategory.AVATAR, "/" + UUID_REGEX);
        private static final Pattern EVENT_COVER = build(UploadCategory.EVENT_COVER, "");
        private static final Pattern MATERIAL = build(UploadCategory.MATERIAL, "");

        private static Pattern build(UploadCategory category, String folderRegex) {
            String extensions = category.types().values().stream().distinct().collect(Collectors.joining("|"));
            return Pattern.compile(
                    Pattern.quote(category.keyPrefix) + folderRegex + "/" + UUID_REGEX + "\\.(" + extensions + ")");
        }
    }

    private static final class Types {
        static final Map<String, String> IMAGES = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

        static final Map<String, String> MATERIALS = Map.ofEntries(
                Map.entry("image/jpeg", "jpg"),
                Map.entry("image/png", "png"),
                Map.entry("image/webp", "webp"),
                Map.entry("application/pdf", "pdf"),
                Map.entry("application/msword", "doc"),
                Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
                Map.entry("application/vnd.ms-powerpoint", "ppt"),
                Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
                Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
                Map.entry("text/plain", "txt"),
                Map.entry("video/mp4", "mp4"),
                Map.entry("video/webm", "webm"));
    }
}
