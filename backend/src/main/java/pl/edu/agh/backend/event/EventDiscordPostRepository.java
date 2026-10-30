package pl.edu.agh.backend.event;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDiscordPostRepository extends JpaRepository<EventDiscordPost, UUID> {}
