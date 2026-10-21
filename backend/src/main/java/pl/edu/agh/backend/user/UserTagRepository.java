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
            select new pl.edu.agh.backend.user.UserTagResponse(t.id, t.name, (
                select count(u) from User u join u.tags assigned
                where assigned = t and u.showName = true and exists (
                    select 1 from Application a where a.applicant = u and a.status = 'APPROVED')))
            from UserTag t
            """)
    List<UserTagResponse> findCatalog();

    @Modifying
    @Query(
            value = "insert into user_skill_tags (id, name) values (:id, :name) on conflict do nothing",
            nativeQuery = true)
    void insertIfAbsent(UUID id, String name);
}
