package pl.edu.agh.backend.topicgroup;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class TopicGroupRequestService {

    private final TopicGroupRequestRepository topicGroupRequestRepository;
    private final TopicGroupRepository topicGroupRepository;
    private final CallerUserService callerUserService;

    @Transactional
    public TopicGroupRequestResponse submit(Caller caller, SubmitTopicGroupRequest request) {
        User requester = callerUserService.getOrCreate(caller);
        if (topicGroupRequestRepository.existsByRequesterIdAndStatus(
                requester.getId(), TopicGroupRequestStatus.UNDER_REVIEW)) {
            throw new TopicGroupRequestAlreadyOpenException();
        }
        String name = request.name().strip();
        if (topicGroupRepository.existsByNameIgnoreCase(name)) {
            throw new TopicGroupNameTakenException(name);
        }
        TopicGroupRequest saved = topicGroupRequestRepository.save(TopicGroupRequest.builder()
                .requester(requester)
                .proposedName(name)
                .description(request.description())
                .build());
        return TopicGroupRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TopicGroupRequestResponse> listMine(Caller caller) {
        return callerUserService
                .findId(caller)
                .map(topicGroupRequestRepository::findByRequesterIdOrderByCreatedAtDesc)
                .orElse(List.of())
                .stream()
                .map(TopicGroupRequestResponse::from)
                .toList();
    }
}
