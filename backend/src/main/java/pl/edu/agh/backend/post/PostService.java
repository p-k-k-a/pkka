package pl.edu.agh.backend.post;

import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> findAllPublished(Caller caller, Pageable pageable) {
        return postRepository
                .findAllByStatusAndAudienceInOrderByPublishedAtDesc(PostStatus.PUBLISHED, audiencesOf(caller), pageable)
                .map(PostSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse findPublishedBySlug(String slug, Caller caller) {
        return postRepository
                .findBySlugAndStatusAndAudienceIn(slug, PostStatus.PUBLISHED, audiencesOf(caller))
                .map(PostResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    static Set<PostAudience> audiencesOf(Caller caller) {
        return caller.hasRole(Roles.VERIFIED_ALUMN) || caller.hasRole(Roles.ADMIN)
                ? EnumSet.allOf(PostAudience.class)
                : EnumSet.of(PostAudience.PUBLIC);
    }
}
