package pl.edu.agh.backend.discord;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.infrastructure.discord.DiscordRateLimitedException;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

class DiscordRoleSyncJobTest {

    private final DiscordGuildMembership guildMembership = mock(DiscordGuildMembership.class);
    private final DiscordRoleSync roleSync = mock(DiscordRoleSync.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DiscordRoleSyncJob job = new DiscordRoleSyncJob(guildMembership, roleSync, userRepository);

    private final User anna = new User();
    private final User piotr = new User();

    @BeforeEach
    void setUp() {
        given(guildMembership.isConfigured()).willReturn(true);
        given(userRepository.findByDiscordIdIsNotNull()).willReturn(List.of(anna, piotr));
    }

    @Test
    void syncsEveryUserWithDiscord() throws InterruptedException {
        job.syncAll();

        verify(roleSync).sync(anna);
        verify(roleSync).sync(piotr);
    }

    @Test
    void doesNothingBeforeDiscordIsSetUp() throws InterruptedException {
        given(guildMembership.isConfigured()).willReturn(false);

        job.syncAll();

        verifyNoInteractions(roleSync, userRepository);
    }

    @Test
    void oneFailureDoesNotStopTheOthers() throws InterruptedException {
        given(roleSync.sync(anna)).willThrow(new RestClientException("Discord is down"));

        job.syncAll();

        verify(roleSync).sync(piotr);
    }

    @Test
    void waitsOutARateLimitAndTriesTheSameUserOnceMore() throws InterruptedException {
        given(roleSync.sync(anna))
                .willThrow(new DiscordRateLimitedException(Duration.ofMillis(1)))
                .willReturn(DiscordStatus.ON_SERVER);

        job.syncAll();

        verify(roleSync, times(2)).sync(anna);
        verify(roleSync).sync(piotr);
    }

    @Test
    void givesUpOnAUserWhoStaysRateLimited() throws InterruptedException {
        given(roleSync.sync(anna)).willThrow(new DiscordRateLimitedException(Duration.ofMillis(1)));

        job.syncAll();

        verify(roleSync, times(2)).sync(anna);
        verify(roleSync).sync(piotr);
    }
}
