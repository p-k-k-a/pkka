package pl.edu.agh.backend.alumni.contact;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    boolean existsByOwnerIdAndContactId(UUID ownerId, UUID contactId);

    @EntityGraph(attributePaths = "contact")
    List<Contact> findByOwnerIdAndContactShowNameTrueOrderByCreatedAtDesc(UUID ownerId);

    void deleteByOwnerIdAndContactId(UUID ownerId, UUID contactId);
}
