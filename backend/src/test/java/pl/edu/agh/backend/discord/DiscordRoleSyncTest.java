package pl.edu.agh.backend.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.infrastructure.discord.DiscordGroupChannels;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.topicgroup.TopicGroup;
import pl.edu.agh.backend.topicgroup.TopicGroupRepository;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class DiscordRoleSyncTest {

    private static final String DISCORD_ID = "999999999999999999";
    private static final String ALUMN_ROLE = "100000000000000001";
    private static final String AI_ROLE = "100000000000000002";
    private static final String CLOUD_ROLE = "100000000000000003";
    private static final String UNMANAGED_ROLE = "100000000000000004";
    private static final String AI_CHANNEL = "200000000000000001";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    private DiscordGuildMembership guildMembership;

    @MockitoBean
    private DiscordGroupChannels groupChannels;

    @Autowired
    private DiscordRoleSync roleSync;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicGroupRepository topicGroupRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private User anna;
    private TopicGroup ai;

    @BeforeEach
    void setUp() {
        anna = user(DISCORD_ID);
        ai = group("Grupa AI", AI_ROLE, AI_CHANNEL, user(null));
        group("Grupa chmurowa", CLOUD_ROLE, null, null);
        given(guildMembership.alumnRoleId()).willReturn(Optional.of(ALUMN_ROLE));
    }

    @Test
    void leavesAUserWithoutDiscordAlone() {
        assertThat(roleSync.sync(user(null))).isEqualTo(DiscordStatus.NOT_LINKED);

        verifyNoInteractions(guildMembership, groupChannels);
    }

    @Test
    void leavesAUserOutsideTheServerAlone() {
        given(guildMembership.roles(DISCORD_ID)).willReturn(Optional.empty());

        assertThat(roleSync.sync(anna)).isEqualTo(DiscordStatus.NOT_ON_SERVER);

        verify(guildMembership, never()).addRole(any(), any());
        verify(guildMembership, never()).removeRole(any(), any());
    }

    @Test
    void givesAnApprovedAlumnTheAlumnRoleAndTheRolesOfTheirGroups() {
        approve(anna);
        join(anna, ai);
        given(guildMembership.roles(DISCORD_ID)).willReturn(Optional.of(List.of(UNMANAGED_ROLE)));

        assertThat(roleSync.sync(anna)).isEqualTo(DiscordStatus.ON_SERVER);

        verify(guildMembership).addRole(DISCORD_ID, ALUMN_ROLE);
        verify(guildMembership).addRole(DISCORD_ID, AI_ROLE);
        verify(guildMembership, never()).removeRole(any(), any());
    }

    @Test
    void takesAwayOnlyTheRolesOfGroupsTheUserIsNotIn() {
        join(anna, ai);
        given(guildMembership.roles(DISCORD_ID)).willReturn(Optional.of(List.of(AI_ROLE, CLOUD_ROLE, UNMANAGED_ROLE)));

        roleSync.sync(anna);

        verify(guildMembership).removeRole(DISCORD_ID, CLOUD_ROLE);
        verify(guildMembership, never()).removeRole(DISCORD_ID, AI_ROLE);
        verify(guildMembership, never()).removeRole(DISCORD_ID, UNMANAGED_ROLE);
        verify(guildMembership, never()).addRole(any(), any());
    }

    @Test
    void neverTakesAwayTheAlumnRole() {
        given(guildMembership.roles(DISCORD_ID)).willReturn(Optional.of(List.of(ALUMN_ROLE)));

        roleSync.sync(anna);

        verify(guildMembership, never()).removeRole(any(), any());
    }

    @Test
    void letsACoordinatorManageMessagesInTheirGroupsChannel() {
        group("Gamedev", "100000000000000005", "200000000000000002", anna);
        given(guildMembership.roles(DISCORD_ID)).willReturn(Optional.of(List.of()));

        roleSync.sync(anna);

        verify(groupChannels).allowManageMessages("200000000000000002", DISCORD_ID);
        verify(groupChannels, never()).allowManageMessages(AI_CHANNEL, DISCORD_ID);
    }

    @Test
    void discordFailuresReachTheCaller() {
        given(guildMembership.roles(DISCORD_ID)).willThrow(new RestClientException("Discord is down"));

        assertThatThrownBy(() -> roleSync.sync(anna)).isInstanceOf(RestClientException.class);
    }

    private User user(String discordId) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setDiscordId(discordId);
        return userRepository.saveAndFlush(user);
    }

    private TopicGroup group(String name, String roleId, String channelId, User coordinator) {
        return topicGroupRepository.saveAndFlush(TopicGroup.builder()
                .name(name)
                .slug(UUID.randomUUID().toString())
                .coordinator(coordinator)
                .discordRoleId(roleId)
                .discordChannelId(channelId)
                .build());
    }

    private void join(User user, TopicGroup group) {
        jdbc.update(
                "insert into topic_group_memberships values (?, ?, ?, now())",
                UUID.randomUUID(),
                group.getId(),
                user.getId());
    }

    private void approve(User applicant) {
        jdbc.update(
                "insert into applications (id, applicant_id, status, discord_vouched, created_at, updated_at)"
                        + " values (?, ?, 'APPROVED', true, now(), now())",
                UUID.randomUUID(),
                applicant.getId());
    }
}
