package pl.edu.agh.backend.topic;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

    @Transactional(readOnly = true)
    public Page<TopicProposalResponse> listMine(Caller caller, Pageable pageable) {
        UUID authorId = callerUserService
                .findId(caller)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return topicProposalRepository
                .findAllByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
                .map(TopicProposalResponse::from);
    }
}
