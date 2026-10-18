package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.infrastructure.discord.DiscordEmbed;
import pl.edu.agh.backend.infrastructure.discord.DiscordMessages;
import pl.edu.agh.backend.notifications.DevicePlatform;
import pl.edu.agh.backend.notifications.DeviceToken;
import pl.edu.agh.backend.notifications.DeviceTokenRepository;
import pl.edu.agh.backend.notifications.expo.ExpoPushClient;
import pl.edu.agh.backend.notifications.expo.SendOutcome;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest(properties = {"app.notifications.reminder-cron=-", "app.notifications.receipt-cron=-"})
@Testcontainers
@Import(DiscordEventPublisherTest.TestSecurityBeans.class)
class DiscordEventPublisherTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String GUILD_ID = "111111111111111111";
    private static final String CHANNEL_ID = "444444444444444444";
    private static final String MESSAGE_ID = "555555555555555555";

    private static final String EVENT_PAGES = "http://localhost:3000/events/";

    private static final long TIMEOUT_MS = 5000;

    private static final long SETTLE_MS = 1000;

    @MockitoBean
    private DiscordMessages discordMessages;

    @MockitoBean
    private ExpoPushClient expoPushClient;

    @Autowired
    private AdminEventService adminEventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventDiscordPostRepository postRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Caller admin = new Caller(UUID.randomUUID().toString(), Set.of(Roles.ADMIN));

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM discord_settings");
        jdbcTemplate.update(
                "INSERT INTO discord_settings (guild_id, announcements_channel_id) VALUES (?, ?)",
                GUILD_ID,
                CHANNEL_ID);
        when(discordMessages.post(any(), any())).thenReturn(Optional.of(MESSAGE_ID));
        when(discordMessages.delete(any(), any())).thenReturn(true);
        when(expoPushClient.send(any())).thenReturn(new SendOutcome(Map.of(), Set.of()));
    }

    @Test
    void aPublicEventIsAnnouncedAndItsPostRemembered() {
        UUID id = create(Audience.PUBLIC);
        String slug = eventRepository.findById(id).orElseThrow().getSlug();

        ArgumentCaptor<DiscordEmbed> embed = ArgumentCaptor.forClass(DiscordEmbed.class);
        verify(discordMessages, timeout(TIMEOUT_MS)).post(eq(CHANNEL_ID), embed.capture());
        assertThat(embed.getValue().title()).isEqualTo("Warsztaty");
        assertThat(embed.getValue().fields())
                .extracting(DiscordEmbed.Field::name, DiscordEmbed.Field::value)
                .contains(
                        tuple("Forma", "Stacjonarnie"),
                        tuple("Miejsce", "D17, sala 1.38"),
                        tuple("Link", "[Zobacz na stronie](" + EVENT_PAGES + slug + "?ref=discord)"));
        awaitUntil(() -> postRepository.existsById(id));
        assertThat(postRepository.findById(id)).hasValueSatisfying(post -> {
            assertThat(post.getChannelId()).isEqualTo(CHANNEL_ID);
            assertThat(post.getMessageId()).isEqualTo(MESSAGE_ID);
        });
    }

    @Test
    void anAlumniEventIsAnnouncedToo() {
        create(Audience.ALL_ALUMNI);

        verify(discordMessages, timeout(TIMEOUT_MS)).post(eq(CHANNEL_ID), any());
    }

    @Test
    void aGroupEventStaysOffDiscord() {
        create(Audience.SPECIFIC_GROUP);

        verify(discordMessages, after(SETTLE_MS).never()).post(any(), any());
    }

    @Test
    void beforeTheBotIsSetUpNothingIsAnnounced() {
        jdbcTemplate.update("DELETE FROM discord_settings");

        create(Audience.PUBLIC);

        verify(discordMessages, after(SETTLE_MS).never()).post(any(), any());
    }

    @Test
    void editingAnAnnouncedEventEditsItsPost() {
        UUID id = createAnnounced();

        adminEventService.update(id, request("Warsztaty AI", Audience.PUBLIC));

        ArgumentCaptor<DiscordEmbed> embed = ArgumentCaptor.forClass(DiscordEmbed.class);
        verify(discordMessages, timeout(TIMEOUT_MS)).edit(eq(CHANNEL_ID), eq(MESSAGE_ID), embed.capture());
        assertThat(embed.getValue().title()).isEqualTo("Warsztaty AI");
    }

    @Test
    void narrowingAnAnnouncedEventToAGroupTakesItsPostDown() {
        UUID id = createAnnounced();

        adminEventService.update(id, request("Warsztaty", Audience.SPECIFIC_GROUP));

        verify(discordMessages, timeout(TIMEOUT_MS)).delete(CHANNEL_ID, MESSAGE_ID);
        awaitUntil(() -> !postRepository.existsById(id));
    }

    @Test
    void deletingAnAnnouncedEventTakesItsPostDown() {
        UUID id = createAnnounced();

        adminEventService.delete(id);

        verify(discordMessages, timeout(TIMEOUT_MS)).delete(CHANNEL_ID, MESSAGE_ID);
        awaitUntil(() -> !postRepository.existsById(id));
    }

    @Test
    void aPostDiscordCouldNotTakeDownIsKept() {
        UUID id = createAnnounced();
        when(discordMessages.delete(any(), any())).thenReturn(false);

        adminEventService.delete(id);

        verify(discordMessages, timeout(TIMEOUT_MS)).delete(CHANNEL_ID, MESSAGE_ID);
        assertThat(postRepository.existsById(id)).isTrue();
    }

    @Test
    void aFailingDiscordTakesNeitherTheEventNorItsPushNotification() {
        registerDevice("ExponentPushToken[alumn]");
        when(discordMessages.post(any(), any())).thenThrow(new IllegalStateException("Discord is down"));

        UUID id = create(Audience.PUBLIC);

        assertThat(eventRepository.existsById(id)).isTrue();
        verify(expoPushClient, timeout(TIMEOUT_MS)).send(any());
        verify(discordMessages, never()).edit(any(), any(), any());
    }

    private UUID create(Audience audience) {
        return adminEventService.create(admin, request("Warsztaty", audience)).id();
    }

    private UUID createAnnounced() {
        UUID id = create(Audience.PUBLIC);
        awaitUntil(() -> postRepository.existsById(id));
        return id;
    }

    private EventRequest request(String title, Audience audience) {
        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        return new EventRequest(
                title,
                "Opis",
                EventType.IN_PERSON,
                startsAt,
                startsAt.plus(2, ChronoUnit.HOURS),
                null,
                "D17, sala 1.38",
                null,
                null,
                audience,
                null,
                null,
                Set.of());
    }

    private void registerDevice(String token) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(userRepository.save(user))
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .build());
    }

    private static void awaitUntil(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("condition was not met within %d ms".formatted(TIMEOUT_MS));
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}
