package pl.edu.agh.backend.topicgroup;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicGroupRequestRepository extends JpaRepository<TopicGroupRequest, UUID> {}
