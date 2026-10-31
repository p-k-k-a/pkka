package pl.edu.agh.backend.storage;

import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Cloudflare R2 through the S3 API (AWS SDK v2). Configured with {@code app.storage.*}, fed from the
 * {@code R2_*} environment variables. The bucket is private: there is no public URL setting.
 *
 * <p>All four settings empty disables storage (presign endpoints answer 503). Some set and some missing is a
 * deployment mistake and fails startup rather than surfacing later as a broken upload.
 */
@Slf4j
@Configuration
class StorageConfig {

    /** R2 ignores the region but the SDK needs one; "auto" is what R2 documents. */
    private static final Region R2_REGION = Region.of("auto");

    private static final Pattern HTTP_URL = Pattern.compile("(?i)^https?://[^\\s/]+.*$");

    @Bean
    StorageService storageService(
            @Value("${app.storage.endpoint:}") String endpoint,
            @Value("${app.storage.bucket:}") String bucket,
            @Value("${app.storage.access-key-id:}") String accessKeyId,
            @Value("${app.storage.secret-access-key:}") String secretAccessKey) {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("R2_ENDPOINT", endpoint);
        settings.put("R2_BUCKET", bucket);
        settings.put("R2_ACCESS_KEY_ID", accessKeyId);
        settings.put("R2_SECRET_ACCESS_KEY", secretAccessKey);
        List<String> missing = settings.entrySet().stream()
                .filter(setting -> setting.getValue().isBlank())
                .map(Map.Entry::getKey)
                .toList();

        if (missing.size() == settings.size()) {
            log.warn("File storage is not configured (R2_* variables are unset); upload endpoints will answer 503");
            return new StorageService(null, "", URI.create("https://unconfigured.invalid"), Clock.systemUTC());
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Incomplete file storage configuration, missing: " + missing);
        }
        if (!HTTP_URL.matcher(endpoint.trim()).matches()) {
            throw new IllegalStateException("R2_ENDPOINT must be an absolute http(s) URL, e.g. https://example.com");
        }
        return new StorageService(
                presigner(endpoint.trim(), accessKeyId.trim(), secretAccessKey.trim()),
                bucket.trim(),
                URI.create(endpoint.trim()),
                Clock.systemUTC());
    }

    /** Signing is local; this never talks to R2. Path-style because R2 buckets are addressed as /bucket/key. */
    static S3Presigner presigner(String endpoint, String accessKeyId, String secretAccessKey) {
        return S3Presigner.builder()
                .region(R2_REGION)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
