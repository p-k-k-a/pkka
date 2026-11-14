package pl.edu.agh.backend.post;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.storage.StorageService;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> findAllPublished(Pageable pageable) {
        return postRepository
                .findAllByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable)
                .map(post -> PostSummaryResponse.from(post, storageService.resolveUrl(post.getCoverImageUrl())));
    }

    @Transactional(readOnly = true)
    public PostResponse findPublishedBySlug(String slug) {
        return postRepository
                .findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .map(post -> PostResponse.from(
                        post,
                        storageService.resolveContent(post.getContent()),
                        storageService.resolveUrl(post.getCoverImageUrl())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
