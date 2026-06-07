package pl.edu.agh.backend.post;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> findAllPublished(Pageable pageable) {
        return postRepository
                .findAllByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable)
                .map(PostSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse findPublishedBySlug(String slug) {
        return postRepository
                .findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .map(PostResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
