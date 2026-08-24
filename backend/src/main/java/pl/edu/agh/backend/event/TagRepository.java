package pl.edu.agh.backend.event;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Set<Tag> findByNameIn(Collection<String> names);
}
