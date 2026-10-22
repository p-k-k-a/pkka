package pl.edu.agh.backend.legal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.legal.LegalDocument;
import pl.edu.agh.backend.legal.LegalDocumentType;

public record LegalDocumentResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) LegalDocumentType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) int version,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Markdown")
        String content,

        @Schema(description = "What changed since the previous version")
        String changeSummary,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Whoever accepted an earlier version must accept this one again")
        boolean requiresReacceptance,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Must be accepted to submit an application")
        boolean required,

        @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt) {

    public static LegalDocumentResponse from(LegalDocument d) {
        return new LegalDocumentResponse(
                d.getId(),
                d.getType(),
                d.getVersion(),
                d.getTitle(),
                d.getContent(),
                d.getChangeSummary(),
                d.isRequiresReacceptance(),
                d.getType().isRequired(),
                d.getPublishedAt());
    }
}
