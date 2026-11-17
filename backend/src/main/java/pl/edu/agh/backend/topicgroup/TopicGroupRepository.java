package pl.edu.agh.backend.topicgroup;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicGroupRepository extends JpaRepository<TopicGroup, UUID> {

    /** Feeds {@code SlugUtils.uniqueSlug} when a group is named after an approved request. */
    boolean existsBySlug(String slug);
}
