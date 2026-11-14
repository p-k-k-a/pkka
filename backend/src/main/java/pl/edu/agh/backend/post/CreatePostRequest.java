package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Markdown. A picture uploaded through `POST /api/admin/files/presign` with purpose "
                        + "`POST_IMAGE` is referenced by its `objectKey` as the image target; the signed URL "
                        + "a read returned for it is accepted too")
        @NotBlank
        String content,

        // Wide enough for the signed download URL an edit form echoes back; what is stored is capped at 500.
        @Size(max = 2000)
        @Schema(
                description = "External http(s) URL, or the `objectKey` of a file uploaded through "
                        + "`POST /api/admin/files/presign`; an uploaded file's signed URL is accepted too. "
                        + "Blank or missing means no cover")
        @Pattern(
                regexp = "(?i)^(https?://|covers/posts/).+$",
                message = "coverImageUrl must be an http(s) URL or the objectKey of an uploaded cover")
        String coverImageUrl,

        PostStatus status) {}
