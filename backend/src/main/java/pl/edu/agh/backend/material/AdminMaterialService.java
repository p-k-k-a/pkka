package pl.edu.agh.backend.material;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.event.EventRepository;

@Service
@RequiredArgsConstructor
public class AdminMaterialService {

    private final MaterialRepository materialRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public Page<AdminMaterialResponse> list(
            String q, Optional<MaterialType> type, Optional<UUID> eventId, Pageable pageable) {
        Specification<Material> spec = Specification.allOf(
                MaterialSpecifications.matchesQuery(q),
                MaterialSpecifications.hasType(type.orElse(null)),
                MaterialSpecifications.hasEventId(eventId.orElse(null)));
        return materialRepository.findAll(spec, pageable).map(AdminMaterialResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminMaterialResponse get(UUID id) {
        return materialRepository
                .findById(id)
                .map(AdminMaterialResponse::from)
                .orElseThrow(MaterialNotFoundException::new);
    }

    @Transactional
    public AdminMaterialResponse create(MaterialRequest request) {
        Material material = new Material();
        apply(material, request.title(), request.description(), request.type(), request.url(), request.eventId());
        return AdminMaterialResponse.from(materialRepository.saveAndFlush(material));
    }

    @Transactional
    public AdminMaterialResponse update(UUID id, MaterialRequest request) {
        Material material = materialRepository.findById(id).orElseThrow(MaterialNotFoundException::new);
        apply(material, request.title(), request.description(), request.type(), request.url(), request.eventId());
        return AdminMaterialResponse.from(materialRepository.saveAndFlush(material));
    }

    @Transactional
    public void delete(UUID id) {
        Material material = materialRepository.findById(id).orElseThrow(MaterialNotFoundException::new);
        materialRepository.delete(material);
    }

    private void apply(
            Material material, String title, String description, MaterialType type, String url, UUID eventId) {
        material.setTitle(title);
        material.setDescription(description);
        material.setType(type);
        material.setUrl(url);
        material.setEvent(resolveEvent(eventId));
    }

    private Event resolveEvent(UUID eventId) {
        if (eventId == null) {
            return null;
        }
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
