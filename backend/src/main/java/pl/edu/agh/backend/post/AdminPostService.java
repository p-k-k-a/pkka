package pl.edu.agh.backend.post;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.infrastructure.slug.SlugUtils;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.storage.AdminUploadPurpose;
import pl.edu.agh.backend.storage.InvalidFileException;
import pl.edu.agh.backend.storage.StorageService;
import pl.edu.agh.backend.user.CallerUserService;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private static final int MAX_STORED_COVER_LENGTH = 500;

    private final PostRepository postRepository;
    private final CallerUserService callerUserService;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public Page<AdminPostListItemResponse> list(Optional<PostStatus> status, Pageable pageable) {
        Page<Post> posts = status.map(value -> postRepository.findAllByStatusOrderByCreatedAtDesc(value, pageable))
                .orElseGet(() -> postRepository.findAllByOrderByCreatedAtDesc(pageable));
        return posts.map(
                post -> AdminPostListItemResponse.from(post, storageService.resolveUrl(post.getCoverImageUrl())));
    }

    @Transactional(readOnly = true)
    public AdminPostResponse get(UUID id) {
        return postRepository.findById(id).map(this::respond).orElseThrow(PostNotFoundException::new);
    }

    @Transactional
    public AdminPostResponse create(Caller caller, CreatePostRequest request) {
        Post post = new Post();
        post.setTitle(request.title());
        post.setSlug(SlugUtils.uniqueSlug(request.title(), postRepository::existsBySlug));
        post.setContent(storageService.toStoredContent(request.content()));
        post.setCoverImageUrl(coverImageReference(request.coverImageUrl()));
        post.setAuthor(callerUserService.getOrCreate(caller));
        if (request.status() == PostStatus.PUBLISHED) {
            post.publish();
        }
        return respond(postRepository.saveAndFlush(post));
    }

    @Transactional
    public AdminPostResponse update(UUID id, UpdatePostRequest request) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        if (post.getStatus() == PostStatus.PUBLISHED && request.status() != PostStatus.PUBLISHED) {
            throw new PostAlreadyPublishedException();
        }
        post.setTitle(request.title());
        post.setContent(storageService.toStoredContent(request.content()));
        post.setCoverImageUrl(coverImageReference(request.coverImageUrl()));
        if (request.status() == PostStatus.PUBLISHED) {
            post.publish();
        }
        return respond(postRepository.saveAndFlush(post));
    }

    @Transactional
    public void delete(UUID id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        postRepository.delete(post);
    }

    private AdminPostResponse respond(Post post) {
        return AdminPostResponse.from(
                post,
                storageService.resolveContent(post.getContent()),
                storageService.resolveUrl(post.getCoverImageUrl()));
    }

    /** External URL as is, or the key of an uploaded cover; a signed URL echoed back from a read is reduced to its key. */
    private String coverImageReference(String coverImageUrl) {
        String reference = storageService.toStoredReference(AdminUploadPurpose.POST_COVER, coverImageUrl);
        if (reference != null && reference.length() > MAX_STORED_COVER_LENGTH) {
            throw new InvalidFileException("coverImageUrl is too long");
        }
        return reference;
    }
}
