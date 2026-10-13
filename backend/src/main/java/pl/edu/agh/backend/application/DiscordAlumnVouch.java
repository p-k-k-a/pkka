package pl.edu.agh.backend.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class DiscordAlumnVouch {

    private final UserRepository userRepository;
    private final DiscordGuildMembership guildMembership;

    public boolean vouchesFor(Caller caller) {
        if (caller.isAnonymous()) {
            return false;
        }
        return userRepository
                .findByKeycloakId(caller.keycloakId())
                .map(User::getDiscordId)
                .map(guildMembership::holdsAlumnRole)
                .orElse(false);
    }
}
