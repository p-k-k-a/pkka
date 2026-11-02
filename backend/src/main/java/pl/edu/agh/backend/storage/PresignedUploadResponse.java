package pl.edu.agh.backend.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Upload the file with `PUT uploadUrl`, the given headers and the file as the raw body")
public record PresignedUploadResponse(
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Short-lived, single-purpose URL to PUT the file to")
        String uploadUrl,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Headers the PUT must send with exactly these values; the URL is signed with them")
        Map<String, String> headers,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description =
                        "Key of the object in the private bucket. After the upload send it back where the file is "
                                + "attached (avatarKey, an event's coverImageUrl, a material's url); read endpoints then "
                                + "return a short-lived download URL for it")
        String objectKey,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "When uploadUrl stops working")
        Instant expiresAt) {}
