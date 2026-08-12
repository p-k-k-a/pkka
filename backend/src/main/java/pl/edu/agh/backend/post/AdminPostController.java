package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@Tag(name = "Admin Posts", description = "Blog post management for administrators")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @GetMapping
    @Operation(summary = "List all posts including drafts, newest first, optionally filtered by status")
    public Page<AdminPostSummaryResponse> listAdminPosts(
            @RequestParam(required = false) PostStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminPostService.list(status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single post with content, regardless of status")
    @ApiResponse(responseCode = "200", description = "Post details")
    @ApiResponse(responseCode = "404", description = "Post not found", content = @Content)
    public AdminPostResponse getAdminPost(@PathVariable UUID id) {
        return adminPostService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a post with a slug generated from the title; status defaults to DRAFT")
    @ApiResponse(responseCode = "201", description = "Post created")
    public AdminPostResponse createAdminPost(
            @Valid @RequestBody CreatePostRequest request, Authentication authentication) {
        return adminPostService.create(authentication, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a post's editable fields and toggle draft/published")
    @ApiResponse(responseCode = "200", description = "Post updated")
    @ApiResponse(responseCode = "404", description = "Post not found", content = @Content)
    public AdminPostResponse updateAdminPost(@PathVariable UUID id, @Valid @RequestBody UpdatePostRequest request) {
        return adminPostService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a post permanently")
    @ApiResponse(responseCode = "204", description = "Post deleted")
    @ApiResponse(responseCode = "404", description = "Post not found", content = @Content)
    public void deleteAdminPost(@PathVariable UUID id) {
        adminPostService.delete(id);
    }
}
