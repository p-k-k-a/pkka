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
        Optional<List<String>> heldRoleIds = guildMembership.roles(discordId);
        if (heldRoleIds.isEmpty()) {
            return DiscordStatus.NOT_ON_SERVER;
        }
        Set<String> wantedRoleIds = wantedRoleIds(user);
        Set<String> groupRoleIds = topicGroupRepository.findDiscordRoleIds();
        for (String roleId : heldRoleIds.get()) {
            if (groupRoleIds.contains(roleId) && !wantedRoleIds.contains(roleId)) {
                guildMembership.removeRole(discordId, roleId);
            }
        }
        for (String roleId : wantedRoleIds) {
            if (!heldRoleIds.get().contains(roleId)) {
                guildMembership.addRole(discordId, roleId);
            }
        }
        for (TopicGroup group : topicGroupRepository.findByCoordinatorIdAndDiscordChannelIdNotNull(user.getId())) {
            groupChannels.allowManageMessages(group.getDiscordChannelId(), discordId);
        }
        return DiscordStatus.ON_SERVER;
    }

    public void removeManagedRoles(String discordId) {
        if (discordId == null || !guildMembership.isConfigured()) {
            return;
        }
        Optional<List<String>> heldRoleIds = guildMembership.roles(discordId);
        if (heldRoleIds.isEmpty()) {
            return;
        }
        Set<String> managedRoleIds = new HashSet<>(topicGroupRepository.findDiscordRoleIds());
        guildMembership.alumnRoleId().ifPresent(managedRoleIds::add);
        for (String roleId : heldRoleIds.get()) {
            if (managedRoleIds.contains(roleId)) {
                guildMembership.removeRole(discordId, roleId);
            }
        }
    }

    private Set<String> wantedRoleIds(User user) {
        Set<String> wantedRoleIds = new HashSet<>(membershipRepository.findDiscordRoleIdsByUserId(user.getId()));
        if (applicationRepository.existsByApplicantKeycloakIdAndStatus(
                user.getKeycloakId(), ApplicationStatus.APPROVED)) {
            guildMembership.alumnRoleId().ifPresent(wantedRoleIds::add);
        }
        return wantedRoleIds;
    }
}
