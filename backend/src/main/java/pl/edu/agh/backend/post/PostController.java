package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Public posts on the faculty blog")
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "List of published posts", description = """
                    Available without authentication. Posts are ordered from newest to oldest.
                    Pagination parameters: page (default 0), size (default 10).
                    """)
    // No @ApiResponse set to 200 - SpringDoc infers Page<PostSummaryResponse>
    // directly from the ResponseEntity<Page<PostSummaryResponse>> return type.
    // @ParameterObject breaks down the Pageable into separate query params (page, size, sort)
    // instead of emitting the single required "pageable" object.
    public ResponseEntity<Page<PostSummaryResponse>> listPosts(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(postService.findAllPublished(pageable));
    }

    @GetMapping("/{slug}")
    @Operation(
            summary = "Single post by slug",
            description = "Available without authentication. Returns 404 if the post does not exist or is a draft.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PostResponse.class)))
    @ApiResponse(responseCode = "404", description = "Post does not exist or is not published", content = @Content)
    public ResponseEntity<PostResponse> getPost(@PathVariable String slug) {
        return ResponseEntity.ok(postService.findPublishedBySlug(slug));
    }
}
