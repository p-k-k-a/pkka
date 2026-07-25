package pl.edu.agh.backend.alumni;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
@Tag(name = "Alumni", description = "Alumni directory — requires VERIFIED_ALUMN role")
public class AlumniController {

    private final AlumniService alumniService;

    @GetMapping("/{id}")
    @Operation(summary = "Get public alumni profile by user id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                description = "No user with the given id",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AlumniProfileResponse getAlumniProfile(@PathVariable UUID id) {
        return alumniService.getProfile(id);
    }
}
