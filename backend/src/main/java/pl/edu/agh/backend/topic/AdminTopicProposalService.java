package pl.edu.agh.backend.topic;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTopicProposalService {

    private final TopicProposalRepository topicProposalRepository;

    @Transactional(readOnly = true)
    public Page<AdminTopicProposalResponse> list(Optional<TopicProposalStatus> status, Pageable pageable) {
        Page<TopicProposal> proposals = status.map(
                        value -> topicProposalRepository.findAllByStatusOrderByCreatedAtDesc(value, pageable))
                .orElseGet(() -> topicProposalRepository.findAllByOrderByCreatedAtDesc(pageable));
        return proposals.map(AdminTopicProposalResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminTopicProposalResponse get(UUID id) {
        return topicProposalRepository
                .findById(id)
                .map(AdminTopicProposalResponse::from)
                .orElseThrow(TopicProposalNotFoundException::new);
    }

    @Transactional
    public AdminTopicProposalResponse updateStatus(UUID id, UpdateTopicProposalStatusRequest request) {
        TopicProposal proposal = topicProposalRepository.findById(id).orElseThrow(TopicProposalNotFoundException::new);
        proposal.setStatus(request.status());
        return AdminTopicProposalResponse.from(topicProposalRepository.saveAndFlush(proposal));
    }
}
