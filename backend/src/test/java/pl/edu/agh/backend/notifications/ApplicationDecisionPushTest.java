package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.backend.notifications.expo.ExpoPushClient;
import pl.edu.agh.backend.notifications.expo.ExpoPushMessage;
import pl.edu.agh.backend.notifications.expo.SendOutcome;

@ExtendWith(MockitoExtension.class)
class ApplicationDecisionPushTest {

    private static final UUID APPLICANT = UUID.randomUUID();

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private ExpoPushClient expoPushClient;

    @Mock
    private PushTicketRepository pushTicketRepository;

    @InjectMocks
    private NotificationService notificationService;

    private List<ExpoPushMessage> sentFor(Runnable push) {
        DeviceToken device =
                DeviceToken.builder().token("ExponentPushToken[ala]").build();
        when(deviceTokenRepository.findByUserIdIn(List.of(APPLICANT))).thenReturn(List.of(device));
        when(expoPushClient.send(any())).thenReturn(new SendOutcome(Map.of(), Set.of()));
        push.run();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExpoPushMessage>> sent = ArgumentCaptor.forClass(List.class);
        verify(expoPushClient).send(sent.capture());
        verify(pushTicketRepository).saveAll(List.of());
        return sent.getValue();
    }

    @Test
    void approval_opensTheProfileOnTheAccountChannel() {
        List<ExpoPushMessage> sent = sentFor(() -> notificationService.pushApplicationApproved(APPLICANT));

        assertThat(sent)
                .containsExactly(new ExpoPushMessage(
                        "ExponentPushToken[ala]",
                        "Wniosek zaakceptowany",
                        "Witamy w Klubie! Uzupełnij profil, żeby inni absolwenci mogli Cię znaleźć.",
                        "account",
                        Map.of("type", "APPLICATION_APPROVED", "target", "profile")));
    }

    @Test
    void rejection_pointsToTheReasonInTheProfile() {
        List<ExpoPushMessage> sent = sentFor(() -> notificationService.pushApplicationRejected(APPLICANT));

        assertThat(sent)
                .containsExactly(new ExpoPushMessage(
                        "ExponentPushToken[ala]",
                        "Wniosek odrzucony",
                        "Sprawdź powód w profilu i złóż wniosek ponownie.",
                        "account",
                        Map.of("type", "APPLICATION_REJECTED", "target", "profile")));
    }
}
