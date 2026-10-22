package pl.edu.agh.backend.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.legal.dto.ConsentResponse;
import pl.edu.agh.backend.legal.dto.GrantConsentRequest;
import pl.edu.agh.backend.legal.dto.MyConsentsResponse;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/me/consents")
@RequiredArgsConstructor
@Tag(name = "Consents", description = "The logged-in user's acceptance of legal documents")
public class ConsentController {

    private final ConsentService consentService;

    @GetMapping
    @Operation(summary = "The user's consents and the documents they must accept again")
    public MyConsentsResponse getMyConsents(Caller caller) {
        return consentService.mine(caller);
    }

    @PostMapping
    @Operation(summary = "Accept the current version of a document")
    @ApiResponse(responseCode = "200", description = "Accepted, or already accepted before")
    @ApiResponse(
            responseCode = "409",
            description = "Not a version in force (reason OUTDATED_DOCUMENT); reload the documents",
            content = @Content)
    public ConsentResponse grantConsent(@Valid @RequestBody GrantConsentRequest request, Caller caller) {
        return consentService.grant(caller, request.documentId());
    }

    @DeleteMapping("/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Withdraw an optional consent")
    @ApiResponse(responseCode = "204", description = "Withdrawn, or never given")
    @ApiResponse(
            responseCode = "409",
            description = "Required consents cannot be withdrawn in the app (reason NOT_WITHDRAWABLE)",
            content = @Content)
    public void withdrawConsent(@PathVariable LegalDocumentType type, Caller caller) {
        consentService.withdraw(caller, type);
    }
}
