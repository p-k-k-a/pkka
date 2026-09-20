package pl.edu.agh.backend.topic;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class TopicProposalService {

    private final TopicProposalRepository topicProposalRepository;
    private final CallerUserService callerUserService;

    @Transactional
    public TopicProposalResponse submit(Caller caller, CreateTopicProposalRequest request) {
        User author = callerUserService.getOrCreate(caller);
        TopicProposal proposal = new TopicProposal();
        proposal.setTitle(request.title());
        proposal.setDescription(request.description());
        proposal.setRationale(request.rationale());
        proposal.setAuthor(author);
        proposal.setStatus(TopicProposalStatus.PENDING);
        return TopicProposalResponse.from(topicProposalRepository.saveAndFlush(proposal));
    }

    /**
     * Unauthenticated callers never reach this method — SecurityConfig already returns 401 for
     * them. A verified-alumn token with no local {@link pl.edu.agh.backend.user.User} row yet
     * (provisioned lazily on first write via {@link CallerUserService#getOrCreate}) is still
     * authenticated and authorized; they simply have zero proposals, so 200 with an empty page is
     * correct. Neither 401 (auth already passed) nor 404 (the collection endpoint exists) fits.
     */
    @Transactional(readOnly = true)
    public Page<TopicProposalResponse> listMine(Caller caller, Pageable pageable) {
        return callerUserService
                .findId(caller)
                .map(authorId -> topicProposalRepository
                        .findAllByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
                        .map(TopicProposalResponse::from))
                .orElseGet(() -> Page.empty(pageable));
    }
}
