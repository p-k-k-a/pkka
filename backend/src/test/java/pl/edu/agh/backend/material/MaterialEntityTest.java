package pl.edu.agh.backend.material;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventType;

@SpringBootTest
@Testcontainers
class MaterialEntityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesCoreFieldsAndOptionalEventLink() {
        Material material = new Material();
        material.setTitle("Nagranie wykładu");
        material.setDescription("Opis");
        material.setType(MaterialType.RECORDING);
        material.setUrl("https://example.com/recording");

        assertThat(material.getTitle()).isEqualTo("Nagranie wykładu");
        assertThat(material.getType()).isEqualTo(MaterialType.RECORDING);
        assertThat(material.getEvent()).isNull();
    }

    @Test
    @Transactional
    void persistsAndReloadsWithLinkedEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("Konferencja " + Instant.now())
                .type(EventType.ONLINE)
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .audience(Audience.PUBLIC)
                .build());

        Material material = new Material();
        material.setTitle("Slajdy");
        material.setType(MaterialType.PRESENTATION);
        material.setUrl("https://example.com/slides.pdf");
        material.setEvent(event);
        var saved = materialRepository.save(material);
        entityManager.flush();
        entityManager.clear();

        Material reloaded = materialRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getEvent()).isNotNull();
        assertThat(reloaded.getEvent().getId()).isEqualTo(event.getId());
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void softDeletingLinkedEventDoesNotBreakMaterialLookup() {
        // Each repository call below commits in its own transaction, mirroring production:
        // the event is soft-deleted in one request, and a later, unrelated request loads the
        // material. That separation matters because Event uses @SQLDelete +
        // @SQLRestriction("deleted_at IS NULL"): the row stays in the table, but every
        // subsequent SELECT (including the FK lookup for Material.event) filters it out.
        // Without @NotFound(action = IGNORE) on Material.event, resolving the association in a
        // fresh persistence context throws EntityNotFoundException, which surfaces as an
        // unhandled 500 on any endpoint that returns this material.
        Event event = eventRepository.save(Event.builder()
                .title("Konferencja usuwana " + Instant.now())
                .type(EventType.ONLINE)
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .audience(Audience.PUBLIC)
                .build());

        Material material = new Material();
        material.setTitle("Nagranie");
        material.setType(MaterialType.RECORDING);
        material.setUrl("https://example.com/recording.mp4");
        material.setEvent(event);
        UUID savedId = materialRepository.save(material).getId();

        eventRepository.delete(event);

        Material reloaded = materialRepository.findById(savedId).orElseThrow();
        assertThat(reloaded.getEvent()).isNull();
    }
}
