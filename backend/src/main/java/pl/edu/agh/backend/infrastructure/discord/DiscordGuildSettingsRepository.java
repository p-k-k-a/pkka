package pl.edu.agh.backend.infrastructure.discord;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscordGuildSettingsRepository extends JpaRepository<DiscordGuildSettings, String> {

    Optional<DiscordGuildSettings> findFirstBy();
}
