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
import pl.edu.agh.backend.storage.AdminUploadPurpose;
import pl.edu.agh.backend.storage.StorageService;

@Service
@RequiredArgsConstructor
public class AdminMaterialService {

    private final MaterialRepository materialRepository;
    private final EventRepository eventRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public Page<AdminMaterialResponse> list(Optional<MaterialType> type, Optional<UUID> eventId, Pageable pageable) {
        Specification<Material> spec = Specification.allOf(
                MaterialSpecifications.hasType(type.orElse(null)),
                MaterialSpecifications.hasEventId(eventId.orElse(null)));
        return materialRepository.findAll(spec, pageable).map(this::respond);
    }

    @Transactional(readOnly = true)
    public AdminMaterialResponse get(UUID id) {
        return materialRepository.findById(id).map(this::respond).orElseThrow(MaterialNotFoundException::new);
    }

    @Transactional
    public AdminMaterialResponse create(MaterialRequest request) {
        Material material = new Material();
        apply(material, request.title(), request.description(), request.type(), request.url(), request.eventId());
        return respond(materialRepository.saveAndFlush(material));
    }

    @Transactional
    public AdminMaterialResponse update(UUID id, MaterialRequest request) {
        Material material = materialRepository.findById(id).orElseThrow(MaterialNotFoundException::new);
        apply(material, request.title(), request.description(), request.type(), request.url(), request.eventId());
        return respond(materialRepository.saveAndFlush(material));
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
        material.setUrl(storageService.toStoredReference(AdminUploadPurpose.MATERIAL, url));
        material.setEvent(resolveEvent(eventId));
    }

    private AdminMaterialResponse respond(Material material) {
        return AdminMaterialResponse.from(material, storageService.resolveUrl(material.getUrl()));
    }

    private Event resolveEvent(UUID eventId) {
        if (eventId == null) {
            return null;
        }
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
