package pl.edu.agh.backend.infrastructure.discord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "discord_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscordGuildSettings {

    @Id
    @Getter
    @Column(name = "guild_id", nullable = false, updatable = false, length = 32)
    private String guildId;

    @Column(name = "announcements_channel_id", length = 32)
    private String announcementsChannelId;

    @Column(name = "alumn_role_id", length = 32)
    private String alumnRoleId;

    DiscordGuildSettings(String guildId, String announcementsChannelId, String alumnRoleId) {
        this.guildId = guildId;
        this.announcementsChannelId = announcementsChannelId;
        this.alumnRoleId = alumnRoleId;
    }

    void configure(Optional<String> channelId, Optional<String> roleId) {
        channelId.ifPresent(id -> announcementsChannelId = id);
        roleId.ifPresent(id -> alumnRoleId = id);
    }

    public Optional<String> getAnnouncementsChannelId() {
        return Optional.ofNullable(announcementsChannelId);
    }

    public Optional<String> getAlumnRoleId() {
        return Optional.ofNullable(alumnRoleId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DiscordGuildSettings other)) {
            return false;
        }
        return guildId != null && guildId.equals(other.guildId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
