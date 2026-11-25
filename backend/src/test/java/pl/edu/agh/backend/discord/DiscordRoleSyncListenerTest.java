package pl.edu.agh.backend.discord;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.topicgroup.TopicGroupMembershipChangedEvent;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

class DiscordRoleSyncListenerTest {

    private static final UUID ANNA_ID = UUID.randomUUID();

    private final DiscordGuildMembership guildMembership = mock(DiscordGuildMembership.class);
    private final DiscordRoleSync roleSync = mock(DiscordRoleSync.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DiscordRoleSyncListener listener =
            new DiscordRoleSyncListener(guildMembership, roleSync, userRepository);

    private final User anna = new User();

    @BeforeEach
    void setUp() {
        given(guildMembership.isConfigured()).willReturn(true);
        given(userRepository.findById(ANNA_ID)).willReturn(Optional.of(anna));
    }

    @Test
    void syncsTheRolesOfTheUserWhoJoinedOrLeft() {
        listener.onTopicGroupMembershipChanged(new TopicGroupMembershipChangedEvent(ANNA_ID));

        verify(roleSync).sync(anna);
    }

    @Test
    void doesNothingBeforeDiscordIsSetUp() {
        given(guildMembership.isConfigured()).willReturn(false);

        listener.onTopicGroupMembershipChanged(new TopicGroupMembershipChangedEvent(ANNA_ID));

        verifyNoInteractions(roleSync, userRepository);
    }

    @Test
    void aDiscordFailureDoesNotReachTheCaller() {
        given(roleSync.sync(anna)).willThrow(new RestClientException("Discord is down"));

        listener.onTopicGroupMembershipChanged(new TopicGroupMembershipChangedEvent(ANNA_ID));

        verify(roleSync).sync(anna);
    }
}
