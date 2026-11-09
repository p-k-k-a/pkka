package pl.edu.agh.backend.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@Tag(name = "Admin Files", description = "Upload of files attached to club content — requires ADMIN role")
public class AdminFileController {

    private final StorageService storageService;

    @PostMapping("/presign")
    @Operation(
            summary = "Get a presigned URL to upload a file straight to object storage",
            description =
                    "The backend does not receive the file. PUT it to `uploadUrl` with the returned headers, then "
                            + "send `objectKey` where the file is attached (a material's `url` or an event's "
                            + "`coverImageUrl`). The bucket is private: read endpoints return a short-lived signed "
                            + "URL for the stored key.")
    @ApiResponse(responseCode = "200", description = "Upload URL issued")
    @ApiResponse(responseCode = "413", description = "File larger than the limit for this purpose", content = @Content)
    @ApiResponse(responseCode = "415", description = "Content type not allowed for this purpose", content = @Content)
    @ApiResponse(responseCode = "503", description = "Object storage is not configured", content = @Content)
    public PresignedUploadResponse presignAdminUpload(@Valid @RequestBody AdminPresignUploadRequest request) {
        return storageService.presignAdminUpload(request.purpose(), request.contentType(), request.sizeBytes());
    }
}
