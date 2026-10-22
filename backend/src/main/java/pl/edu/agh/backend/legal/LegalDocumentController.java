package pl.edu.agh.backend.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.legal.dto.LegalDocumentResponse;

@RestController
@RequestMapping("/api/public/legal-documents")
@RequiredArgsConstructor
@Tag(name = "Legal documents", description = "Terms, privacy notice and consent wordings in force")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping
    @Operation(summary = "The version in force of every legal document")
    public List<LegalDocumentResponse> listCurrentLegalDocuments() {
        return legalDocumentService.current();
    }

    @GetMapping("/{type}")
    @Operation(summary = "The version in force of one legal document")
    @ApiResponse(responseCode = "200", description = "The current version")
    @ApiResponse(responseCode = "404", description = "No version published yet", content = @Content)
    public LegalDocumentResponse getCurrentLegalDocument(@PathVariable LegalDocumentType type) {
        return legalDocumentService.current(type);
    }
}
