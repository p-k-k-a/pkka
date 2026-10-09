package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.registration.RegistrationPromotedEvent;

class RegistrationPromotedNotifierTest {

    private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final ExpoPushClient expoPushClient = mock(ExpoPushClient.class);
    private final RegistrationPromotedNotifier notifier =
            new RegistrationPromotedNotifier(deviceTokenRepository, eventRepository, expoPushClient);

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void notifiesEveryDeviceOfThePromotedUser() {
        when(deviceTokenRepository.findAllByUserId(userId))
                .thenReturn(List.of(device("ExponentPushToken[phone]"), device("ExponentPushToken[tablet]")));
        Event event = mock(Event.class);
        when(event.getTitle()).thenReturn("Warsztaty z Rusta");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        notifier.onRegistrationPromoted(new RegistrationPromotedEvent(eventId, userId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PushMessage>> sent = ArgumentCaptor.forClass(List.class);
        verify(expoPushClient).send(sent.capture());
        assertThat(sent.getValue())
                .extracting(PushMessage::to)
                .containsExactly("ExponentPushToken[phone]", "ExponentPushToken[tablet]");
        assertThat(sent.getValue().getFirst().body()).contains("Warsztaty z Rusta");
        assertThat(sent.getValue().getFirst().data())
                .containsEntry("type", RegistrationPromotedNotifier.TYPE)
                .containsEntry("eventId", eventId.toString());
    }

    @Test
    void staysQuietForUsersWithoutDevices() {
        when(deviceTokenRepository.findAllByUserId(userId)).thenReturn(List.of());

        notifier.onRegistrationPromoted(new RegistrationPromotedEvent(eventId, userId));

        verify(expoPushClient, never()).send(any());
    }

    private static DeviceToken device(String token) {
        return DeviceToken.builder()
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .build();
    }
}
