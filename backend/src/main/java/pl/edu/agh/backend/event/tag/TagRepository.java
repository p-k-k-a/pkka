package pl.edu.agh.backend.event.tag;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    Set<Tag> findForShareByNameIn(Collection<String> names);

    @Modifying
    @Query(
            value = "insert into tags (id, name, created_by) values (:id, :name, :createdBy) on conflict do nothing",
            nativeQuery = true)
    void insertIfAbsent(UUID id, String name, UUID createdBy);

    @Query(value = "select id from tags where created_by is not null for update", nativeQuery = true)
    List<UUID> lockAdminCreated();

    @Modifying(flushAutomatically = true)
    @Query(
            value = "delete from tags t where t.created_by is not null and not exists ("
                    + "select 1 from event_tags et join events e on e.id = et.event_id "
                    + "where et.tag_id = t.id and e.deleted_at is null)",
            nativeQuery = true)
    void deleteUnusedAdminCreated();
}
