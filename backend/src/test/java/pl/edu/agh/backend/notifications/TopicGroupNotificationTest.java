package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.notifications.expo.ExpoPushClient;
import pl.edu.agh.backend.notifications.expo.ExpoPushMessage;
import pl.edu.agh.backend.notifications.expo.SendOutcome;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.topicgroup.AdminTopicGroupRequestService;
import pl.edu.agh.backend.topicgroup.SubmitTopicGroupRequest;
import pl.edu.agh.backend.topicgroup.TopicGroup;
import pl.edu.agh.backend.topicgroup.TopicGroupRepository;
import pl.edu.agh.backend.topicgroup.TopicGroupRequestService;
import pl.edu.agh.backend.topicgroup.TopicGroupService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** No {@code @Transactional}: notifications go out on commit, which a rolled-back test never reaches. */
@SpringBootTest(
        properties = {
            "app.notifications.reminder-cron=-",
            "app.notifications.receipt-cron=-",
            "app.notifications.group-digest-cron=-"
        })
@Testcontainers
@Import(TopicGroupNotificationTest.TestSecurityBeans.class)
class TopicGroupNotificationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final long SEND_TIMEOUT_MS = 5000;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private ExpoPushClient expoPushClient;

    @Autowired
    private TopicGroupRequestService topicGroupRequestService;

    @Autowired
    private AdminTopicGroupRequestService adminTopicGroupRequestService;

    @Autowired
    private TopicGroupService topicGroupService;

    @Autowired
    private TopicGroupRepository topicGroupRepository;

    @Autowired
    private TopicGroupJoinDigestJob topicGroupJoinDigestJob;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    private final Caller admin = new Caller(UUID.randomUUID().toString(), Set.of("ADMIN"));

    @BeforeEach
    void setUp() {
        reset(mailService, expoPushClient);
        when(mailService.send(anyString(), anyString(), anyString(), anyMap())).thenReturn(true);
        when(expoPushClient.send(any())).thenReturn(new SendOutcome(Map.of(), Set.of()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvingARequest_tellsTheRequesterTheGroupIsTheirs() {
        User anna = userWithDevice("anna");
        String name = uniqueName();

        UUID requestId = submit(anna, name);
        adminTopicGroupRequestService.approve(admin, requestId);

        String slug = topicGroupRepository.findAll().stream()
                .filter(group -> group.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getSlug();
        ExpoPushMessage push = capturePush();
        assertThat(push.to()).isEqualTo(token(anna));
        assertThat(push.title()).isEqualTo("Twoja grupa „%s” została utworzona".formatted(name));
        assertThat(push.channelId()).isEqualTo("groups");
        assertThat(push.data()).containsEntry("type", "TOPIC_GROUP_CREATED").containsEntry("target", slug);

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(mailService, timeout(SEND_TIMEOUT_MS))
                .send(
                        eq(anna.getEmail()),
                        eq("Twoja grupa „%s” została utworzona".formatted(name)),
                        eq("topic-group"),
                        variables.capture());
        assertThat(variables.getValue()).containsEntry("linkPath", "/dashboard/groups/" + slug);
        assertThat((List<String>) variables.getValue().get("lines"))
                .anyMatch(line -> line.contains("koordynatorem"))
                .anyMatch(line -> line.contains("Discord"));
    }

    @Test
    void aFailingPush_breaksNeitherTheApprovalNorTheMail() {
        when(expoPushClient.send(any())).thenThrow(new IllegalStateException("Expo is down"));
        User anna = userWithDevice("anna");

        adminTopicGroupRequestService.approve(admin, submit(anna, uniqueName()));

        verify(expoPushClient, timeout(SEND_TIMEOUT_MS)).send(any());
        verify(mailService, timeout(SEND_TIMEOUT_MS))
                .send(eq(anna.getEmail()), anyString(), eq("topic-group"), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectingARequest_sendsTheReasonInFullByMailAndShortenedByPush() {
        User anna = userWithDevice("anna");
        String name = uniqueName();
        String reason = "Taka grupa już działa. " + "x".repeat(300);

        UUID requestId = submit(anna, name);
        adminTopicGroupRequestService.reject(admin, requestId, reason);

        ExpoPushMessage push = capturePush();
        assertThat(push.title()).isEqualTo("Wniosek o grupę „%s” odrzucony".formatted(name));
        assertThat(push.body()).startsWith("Powód: Taka grupa już działa.").endsWith("…");
        assertThat(push.body().length()).isLessThanOrEqualTo(160);
        assertThat(push.data())
                .containsEntry("type", "TOPIC_GROUP_REQUEST_REJECTED")
                .containsEntry("target", requestId.toString());

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(mailService, timeout(SEND_TIMEOUT_MS))
                .send(
                        eq(anna.getEmail()),
                        eq("Wniosek o grupę „%s” odrzucony".formatted(name)),
                        eq("topic-group"),
                        variables.capture());
        assertThat((List<String>) variables.getValue().get("lines")).contains(reason);
    }

    @Test
    void joinDigest_countsNewMembersOnceAndLeavesOutTheCoordinatorAndQuietGroups() {
        User anna = userWithDevice("anna");
        TopicGroup group = groupCoordinatedBy(anna);
        groupCoordinatedBy(userWithDevice("quiet"));
        joinAs(anna, group);
        joinAs(saveUser("piotr"), group);
        joinAs(saveUser("ola"), group);
        joinAs(saveUser("jan"), group);

        topicGroupJoinDigestJob.sendDigests();

        String title = "3 nowe osoby w grupie „%s”".formatted(group.getName());
        verify(mailService).send(eq(anna.getEmail()), eq(title), eq("topic-group"), anyMap());
        verify(mailService, times(1)).send(anyString(), anyString(), anyString(), anyMap());
        ExpoPushMessage push = capturePush();
        assertThat(push.to()).isEqualTo(token(anna));
        assertThat(push.title()).isEqualTo(title);
        assertThat(push.data())
                .containsEntry("type", "TOPIC_GROUP_NEW_MEMBERS")
                .containsEntry("target", group.getSlug());

        reset(mailService);
        topicGroupJoinDigestJob.sendDigests();
        verify(mailService, never()).send(anyString(), anyString(), anyString(), anyMap());

        joinAs(saveUser("ewa"), group);
        topicGroupJoinDigestJob.sendDigests();
        verify(mailService)
                .send(
                        eq(anna.getEmail()),
                        eq("1 nowa osoba w grupie „%s”".formatted(group.getName())),
                        eq("topic-group"),
                        anyMap());
    }

    @Test
    void joinDigest_usesPolishPluralForms() {
        assertThat(TopicGroupJoinDigestJob.newMembers(1)).isEqualTo("1 nowa osoba");
        assertThat(TopicGroupJoinDigestJob.newMembers(4)).isEqualTo("4 nowe osoby");
        assertThat(TopicGroupJoinDigestJob.newMembers(5)).isEqualTo("5 nowych osób");
        assertThat(TopicGroupJoinDigestJob.newMembers(12)).isEqualTo("12 nowych osób");
        assertThat(TopicGroupJoinDigestJob.newMembers(22)).isEqualTo("22 nowe osoby");
    }

    @Test
    void joinDigest_skipsAGroupWithoutACoordinator() {
        TopicGroup orphan = topicGroupRepository.save(TopicGroup.builder()
                .name(uniqueName())
                .slug("bez-koordynatora-" + UUID.randomUUID())
                .build());
        joinAs(saveUser("piotr"), orphan);

        topicGroupJoinDigestJob.sendDigests();

        verify(mailService, never()).send(anyString(), anyString(), anyString(), anyMap());
        verify(expoPushClient, never()).send(any());
    }

    private UUID submit(User requester, String name) {
        return topicGroupRequestService
                .submit(caller(requester), new SubmitTopicGroupRequest(name, "Opis grupy"))
                .id();
    }

    private void joinAs(User user, TopicGroup group) {
        topicGroupService.join(caller(user), group.getId());
    }

    private TopicGroup groupCoordinatedBy(User coordinator) {
        String name = uniqueName();
        return topicGroupRepository.save(TopicGroup.builder()
                .name(name)
                .slug("grupa-" + UUID.randomUUID())
                .coordinator(coordinator)
                .build());
    }

    private static Caller caller(User user) {
        return new Caller(user.getKeycloakId(), Set.of("VERIFIED_ALUMN"));
    }

    private static String uniqueName() {
        return "Grupa " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String token(User user) {
        return "ExponentPushToken[" + user.getId() + "]";
    }

    /** Users from earlier tests stay in the database, so each test uses its own address. */
    private User saveUser(String name) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setEmail(name + "-" + UUID.randomUUID() + "@example.com");
        return userRepository.save(user);
    }

    private User userWithDevice(String name) {
        User user = saveUser(name);
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token(token(user))
                .platform(DevicePlatform.ANDROID)
                .build());
        return user;
    }

    @SuppressWarnings("unchecked")
    private ExpoPushMessage capturePush() {
        ArgumentCaptor<List<ExpoPushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(expoPushClient, timeout(SEND_TIMEOUT_MS)).send(captor.capture());
        return captor.getValue().getFirst();
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
