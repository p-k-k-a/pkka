package pl.edu.agh.backend.topicgroup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.infrastructure.discord.DiscordGroupChannels;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(TestSecurityConfig.class)
class TopicGroupDiscordEndpointTest {

    private static final String MY_REQUESTS = "/api/alumni/me/topic-group-requests";
    private static final String ADMIN_REQUESTS = "/api/admin/topic-group-requests";
    private static final String ADMIN_GROUPS = "/api/admin/topic-groups";

    private static final String ROLE_ID = "222222222222222222";
    private static final String CHANNEL_ID = "444444444444444444";
    private static final String OWNER_DISCORD_ID = "999999999999999999";

    private static final long TIMEOUT_MS = 5000;
    private static final long SETTLE_MS = 1000;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    private DiscordGroupChannels discordGroupChannels;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicGroupRepository topicGroupRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM topic_group_requests");
        jdbcTemplate.update("DELETE FROM topic_groups");
        admin = user("Adam", "Admin", null);
        when(discordGroupChannels.isConfigured()).thenReturn(true);
        when(discordGroupChannels.createRole(any())).thenReturn(Optional.of(ROLE_ID));
        when(discordGroupChannels.createChannel(any(), any())).thenReturn(Optional.of(CHANNEL_ID));
        when(discordGroupChannels.allowManageMessages(any(), any())).thenReturn(true);
    }

    @Test
    void approvingCreatesTheRoleAndChannelAndLetsTheOwnerManageMessages() throws Exception {
        approve(submittedId(user("Anna", "Nowak", OWNER_DISCORD_ID), "Grupa AI"));

        verify(discordGroupChannels, timeout(TIMEOUT_MS)).createRole("Grupa AI");
        verify(discordGroupChannels, timeout(TIMEOUT_MS)).createChannel("grupa-ai", ROLE_ID);
        verify(discordGroupChannels, timeout(TIMEOUT_MS)).allowManageMessages(CHANNEL_ID, OWNER_DISCORD_ID);
        listGroups()
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Grupa AI"))
                .andExpect(jsonPath("$.content[0].owner.firstName").value("Anna"))
                .andExpect(jsonPath("$.content[0].discord").value("LINKED"));
    }

    @Test
    void anOwnerWithoutDiscordGetsNoPermissionOnTheChannel() throws Exception {
        approve(submittedId(user("Piotr", "Zieliński", null), "Grupa AI"));

        verify(discordGroupChannels, timeout(TIMEOUT_MS)).createChannel("grupa-ai", ROLE_ID);
        verify(discordGroupChannels, after(SETTLE_MS).never()).allowManageMessages(any(), any());
    }

    @Test
    void withoutDiscordSetUpTheGroupIsCreatedWithoutAChannel() throws Exception {
        when(discordGroupChannels.isConfigured()).thenReturn(false);

        approve(submittedId(user("Anna", "Nowak", OWNER_DISCORD_ID), "Grupa AI"));

        verify(discordGroupChannels, after(SETTLE_MS).never()).createRole(any());
        listGroups().andExpect(jsonPath("$.content[0].discord").value("NOT_CONFIGURED"));
    }

    @Test
    void whenTheChannelFailsTheRoleIsKeptAndARetryCreatesOnlyTheChannel() throws Exception {
        when(discordGroupChannels.createChannel(any(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(CHANNEL_ID));

        approve(submittedId(user("Anna", "Nowak", OWNER_DISCORD_ID), "Grupa AI"));

        verify(discordGroupChannels, timeout(TIMEOUT_MS)).createChannel("grupa-ai", ROLE_ID);
        String groupId = JsonPath.read(
                listGroups()
                        .andExpect(jsonPath("$.content[0].discord").value("MISSING"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.content[0].id");

        retry(groupId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discord").value("LINKED"));
        verify(discordGroupChannels, times(1)).createRole(any());
        verify(discordGroupChannels, times(2)).createChannel("grupa-ai", ROLE_ID);
        verify(discordGroupChannels).allowManageMessages(CHANNEL_ID, OWNER_DISCORD_ID);
    }

    @Test
    void retryingALinkedGroupSendsNothingToDiscord() throws Exception {
        TopicGroup group = topicGroupRepository.save(TopicGroup.builder()
                .name("Grupa AI")
                .slug("grupa-ai")
                .owner(user("Anna", "Nowak", OWNER_DISCORD_ID))
                .discordRoleId(ROLE_ID)
                .discordChannelId(CHANNEL_ID)
                .build());

        retry(group.getId().toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discord").value("LINKED"));

        verify(discordGroupChannels, never()).createRole(any());
        verify(discordGroupChannels, never()).createChannel(any(), any());
        verify(discordGroupChannels, never()).allowManageMessages(any(), any());
    }

    @Test
    void retryingAnUnknownGroupReturns404() throws Exception {
        retry(UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }

    @Test
    void onlyAdminsSeeTheGroups() throws Exception {
        mockMvc.perform(get(ADMIN_GROUPS).with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isForbidden());
    }

    private ResultActions listGroups() throws Exception {
        return mockMvc.perform(get(ADMIN_GROUPS).with(JwtTestSupport.asAdmin(admin.getKeycloakId())))
                .andExpect(status().isOk());
    }

    private ResultActions retry(String groupId) throws Exception {
        return mockMvc.perform(post(ADMIN_GROUPS + "/{id}/discord", groupId)
                .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                .with(csrf()));
    }

    private void approve(String requestId) throws Exception {
        mockMvc.perform(post(ADMIN_REQUESTS + "/{id}/approve", requestId)
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    private String submittedId(User requester, String name) throws Exception {
        String body = mockMvc.perform(post(MY_REQUESTS)
                        .with(JwtTestSupport.asVerifiedAlumn(requester.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "description": "Opis"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private User user(String firstName, String lastName, String discordId) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDiscordId(discordId);
        return userRepository.save(user);
    }
}
