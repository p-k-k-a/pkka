package pl.edu.agh.backend.user;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserTagRepository extends JpaRepository<UserTag, UUID> {

    @Query("select t from UserTag t where lower(t.name) in :keys")
    List<UserTag> findByNameKeyIn(Collection<String> keys);

    @Query("""
            select new pl.edu.agh.backend.user.UserTagCatalogResponse(t.id, t.name, (
                select count(u) from User u join u.tags assigned
                where assigned = t and u.showName = true and exists (
                    select 1 from Application a where a.applicant = u and a.status = 'APPROVED')))
            from UserTag t
            """)
    List<UserTagCatalogResponse> findCatalog();

    @Modifying
    @Query(
            value = "insert into user_skill_tags (id, name, created_by) values (:id, :name, :createdBy) "
                    + "on conflict do nothing",
            nativeQuery = true)
    void insertIfAbsent(UUID id, String name, UUID createdBy);

    @Modifying(flushAutomatically = true)
    @Query(
            value = "delete from user_skill_tags t where t.id in :ids and t.created_by is not null "
                    + "and not exists (select 1 from user_tags ut where ut.tag_id = t.id)",
            nativeQuery = true)
    void deleteUnusedUserCreated(Collection<UUID> ids);
}
