package pl.edu.agh.backend.post;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.UserProvisioningService;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final PostSlugGenerator slugGenerator;
    private final UserRepository userRepository;
    private final UserPrincipalExtractor principalExtractor;
    private final UserProvisioningService userProvisioningService;

    @Transactional(readOnly = true)
    public Page<AdminPostSummaryResponse> list(PostStatus status, Pageable pageable) {
        Page<Post> posts = status == null
                ? postRepository.findAllByOrderByCreatedAtDesc(pageable)
                : postRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        return posts.map(AdminPostSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminPostResponse get(UUID id) {
        return postRepository.findById(id).map(AdminPostResponse::from).orElseThrow(PostNotFoundException::new);
    }

    @Transactional
    public AdminPostResponse create(Authentication authentication, CreatePostRequest request) {
        Post post = new Post();
        post.setTitle(request.title());
        post.setSlug(slugGenerator.generateUniqueSlug(request.title()));
        post.setContent(request.content());
        post.setAuthor(resolveAuthor(authentication));
        if (request.status() == PostStatus.PUBLISHED) {
            post.publish();
        }
        return AdminPostResponse.from(postRepository.saveAndFlush(post));
    }

    @Transactional
    public AdminPostResponse update(UUID id, UpdatePostRequest request) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        post.setTitle(request.title());
        post.setContent(request.content());
        if (request.status() == PostStatus.PUBLISHED) {
            post.publish();
        } else {
            post.unpublish();
        }
        return AdminPostResponse.from(postRepository.saveAndFlush(post));
    }

    @Transactional
    public void delete(UUID id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        postRepository.delete(post);
    }

    private User resolveAuthor(Authentication authentication) {
        var info = principalExtractor
                .extract(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        userProvisioningService.provisionIfAbsent(info);
        return userRepository
                .findByKeycloakId(info.keycloakId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
