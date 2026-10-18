package pl.edu.agh.backend.event.tag;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Set<Tag> findByNameIn(Collection<String> names);

    @Modifying
    @Query(value = "insert into tags (id, name) values (:id, :name) on conflict do nothing", nativeQuery = true)
    void insertIfAbsent(UUID id, String name);
}
