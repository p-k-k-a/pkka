package pl.edu.agh.backend.post;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final PostSlugGenerator slugGenerator;
    private final CallerUserService callerUserService;

    @Transactional(readOnly = true)
    public Page<AdminPostListItemResponse> list(Optional<PostStatus> status, Pageable pageable) {
        Page<Post> posts = status.map(value -> postRepository.findAllByStatusOrderByCreatedAtDesc(value, pageable))
                .orElseGet(() -> postRepository.findAllByOrderByCreatedAtDesc(pageable));
        return posts.map(AdminPostListItemResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminPostResponse get(UUID id) {
        return postRepository.findById(id).map(AdminPostResponse::from).orElseThrow(PostNotFoundException::new);
    }

    @Transactional
    public AdminPostResponse create(Caller caller, CreatePostRequest request) {
        Post post = new Post();
        post.setTitle(request.title());
        post.setSlug(slugGenerator.generateUniqueSlug(request.title()));
        post.setContent(request.content());
        post.setAuthor(callerUserService.getOrCreate(caller));
        if (request.status() == PostStatus.PUBLISHED) {
            post.publish();
        }
        return AdminPostResponse.from(postRepository.saveAndFlush(post));
    }

    @Transactional
    public AdminPostResponse update(UUID id, UpdatePostRequest request) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        if (post.getStatus() == PostStatus.PUBLISHED && request.status() != PostStatus.PUBLISHED) {
            throw new PostAlreadyPublishedException();
        }
        post.setTitle(request.title());
        post.setContent(request.content());
        if (request.status() == PostStatus.PUBLISHED) {
            post.publish();
        }
        return AdminPostResponse.from(postRepository.saveAndFlush(post));
    }

    @Transactional
    public void delete(UUID id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        postRepository.delete(post);
    }
}
