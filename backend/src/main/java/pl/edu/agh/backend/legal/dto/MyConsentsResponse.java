package pl.edu.agh.backend.legal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

public record MyConsentsResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED, description = "The latest accepted version of each type")
        List<ConsentResponse> consents,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Current versions the user must accept before using the app again")
        List<LegalDocumentResponse> pendingReacceptance) {}
