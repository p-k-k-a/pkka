package pl.edu.agh.backend.legal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.legal.ConsentRecord;
import pl.edu.agh.backend.legal.LegalDocumentType;

public record ConsentResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID documentId,
        @Schema(requiredMode = RequiredMode.REQUIRED) LegalDocumentType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) int version,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant grantedAt) {

    public static ConsentResponse from(ConsentRecord c) {
        return new ConsentResponse(
                c.getDocument().getId(),
                c.getDocument().getType(),
                c.getDocument().getVersion(),
                c.getDocument().getTitle(),
                c.getGrantedAt());
    }
}
