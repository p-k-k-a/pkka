package pl.edu.agh.backend.user;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import pl.edu.agh.backend.infrastructure.mail.MailService;

/**
 * Sends the welcome mail once per account. The row is claimed before sending, so two logins racing each other
 * send one mail; a failed send gives the claim back and the next login tries again.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeMailService {

    private final UserRepository userRepository;
    private final MailService mailService;

    @Async
    @TransactionalEventListener
    public void onWelcomeMailDue(WelcomeMailDueEvent due) {
        if (!mailService.isEnabled() || userRepository.claimWelcomeMail(due.userId()) == 0) {
            return;
        }
        boolean sent = userRepository
                .findById(due.userId())
                .filter(user -> StringUtils.hasText(user.getEmail()))
                .map(user -> mailService.send(
                        user.getEmail(),
                        "Witamy w PKKA",
                        "welcome",
                        Map.of("firstName", user.getFirstName() == null ? "" : user.getFirstName())))
                .orElse(false);
        if (!sent) {
            userRepository.releaseWelcomeMail(due.userId());
            log.info("Welcome mail for user {} not sent, will retry on the next login", due.userId());
        }
    }
}
