package pl.edu.agh.backend.discord;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.infrastructure.discord.DiscordGroupChannels;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.topicgroup.TopicGroup;
import pl.edu.agh.backend.topicgroup.TopicGroupMembershipRepository;
import pl.edu.agh.backend.topicgroup.TopicGroupRepository;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class DiscordRoleSync {

    private final DiscordGuildMembership guildMembership;
    private final DiscordGroupChannels groupChannels;
    private final ApplicationRepository applicationRepository;
    private final TopicGroupRepository topicGroupRepository;
    private final TopicGroupMembershipRepository membershipRepository;

    public DiscordStatus sync(User user) {
        String discordId = user.getDiscordId();
        if (discordId == null) {
            return DiscordStatus.NOT_LINKED;
        }
        Optional<List<String>> held = guildMembership.roles(discordId);
        if (held.isEmpty()) {
            return DiscordStatus.NOT_ON_SERVER;
        }
        Set<String> wanted = wantedRoles(user);
        Set<String> groupRoles = topicGroupRepository.findDiscordRoleIds();
        for (String role : held.get()) {
            if (groupRoles.contains(role) && !wanted.contains(role)) {
                guildMembership.removeRole(discordId, role);
            }
        }
        for (String role : wanted) {
            if (!held.get().contains(role)) {
                guildMembership.addRole(discordId, role);
            }
        }
        for (TopicGroup group : topicGroupRepository.findByCoordinatorIdAndDiscordChannelIdNotNull(user.getId())) {
            groupChannels.allowManageMessages(group.getDiscordChannelId(), discordId);
        }
        return DiscordStatus.ON_SERVER;
    }

    private Set<String> wantedRoles(User user) {
        Set<String> wanted = new HashSet<>(membershipRepository.findDiscordRoleIdsByUserId(user.getId()));
        if (applicationRepository.existsByApplicantKeycloakIdAndStatus(
                user.getKeycloakId(), ApplicationStatus.APPROVED)) {
            guildMembership.alumnRoleId().ifPresent(wanted::add);
        }
        return wanted;
    }
}
