package pl.edu.agh.backend.alumni.contact;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/alumni/me/contacts")
@RequiredArgsConstructor
@Tag(name = "Alumni Contacts", description = "The caller's private contact list — requires VERIFIED_ALUMN role")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @Operation(
            summary = "List own contacts",
            description = "Newest first. Nobody else can see this list. Contacts who hide their name are left out, "
                    + "the same as in the directory, and come back once they show it again.")
    public List<ContactResponse> listContacts(Caller caller) {
        return contactService.list(caller);
    }

    @PutMapping("/{alumnId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Add an alumn to own contacts",
            description = "Idempotent: adding someone who is already a contact changes nothing.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The alumn is in the caller's contacts"),
        @ApiResponse(
                responseCode = "400",
                description = "The caller tried to add themselves",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "No user with the given id, or that user is not a verified alumnus",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void addContact(@PathVariable UUID alumnId, Caller caller) {
        contactService.add(caller, alumnId);
    }

    @DeleteMapping("/{alumnId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove an alumn from own contacts",
            description = "Idempotent: removing someone who is not a contact changes nothing.")
    @ApiResponse(responseCode = "204", description = "The alumn is not in the caller's contacts")
    public void removeContact(@PathVariable UUID alumnId, Caller caller) {
        contactService.remove(caller, alumnId);
    }
}
