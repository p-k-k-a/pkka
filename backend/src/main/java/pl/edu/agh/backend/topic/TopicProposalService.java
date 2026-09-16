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
     * A valid, verified-alumn token is enough to reach this endpoint (see SecurityConfig); the
     * local {@link pl.edu.agh.backend.user.User} row is only created lazily on the first write
     * (see {@link CallerUserService#getOrCreate}). So a caller who has never submitted anything
     * yet is a legitimate, authenticated caller with zero proposals — not an error — and gets an
     * empty page instead of a 401.
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
