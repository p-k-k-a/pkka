package pl.edu.agh.backend.topicgroup;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.infrastructure.slug.SlugUtils;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class AdminTopicGroupRequestService {

    private final TopicGroupRequestRepository topicGroupRequestRepository;
    private final TopicGroupRepository topicGroupRepository;
    private final CallerUserService callerUserService;

    @Transactional(readOnly = true)
    public Page<TopicGroupRequestResponse> list(TopicGroupRequestStatus status, Pageable pageable) {
        return topicGroupRequestRepository
                .findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(TopicGroupRequestResponse::from);
    }

    @Transactional
    public TopicGroupRequestResponse approve(Caller caller, UUID requestId) {
        User reviewer = callerUserService.getOrCreate(caller);
        TopicGroupRequest request =
                topicGroupRequestRepository.findById(requestId).orElseThrow(TopicGroupRequestNotFoundException::new);

        request.ensureUnderReview();
        if (topicGroupRepository.existsByNameIgnoreCase(request.getProposedName())) {
            throw new TopicGroupNameTakenException(request.getProposedName());
        }
        TopicGroup group = topicGroupRepository.save(TopicGroup.builder()
                .name(request.getProposedName())
                .slug(SlugUtils.uniqueSlug(request.getProposedName(), topicGroupRepository::existsBySlug))
                .description(request.getDescription())
                .owner(request.getRequester())
                .build());
        request.approve(reviewer, group);
        return TopicGroupRequestResponse.from(topicGroupRequestRepository.saveAndFlush(request));
    }

    @Transactional
    public TopicGroupRequestResponse reject(Caller caller, UUID requestId, String reason) {
        User reviewer = callerUserService.getOrCreate(caller);
        TopicGroupRequest request =
                topicGroupRequestRepository.findById(requestId).orElseThrow(TopicGroupRequestNotFoundException::new);

        request.reject(reviewer, reason);
        return TopicGroupRequestResponse.from(topicGroupRequestRepository.saveAndFlush(request));
    }
}
