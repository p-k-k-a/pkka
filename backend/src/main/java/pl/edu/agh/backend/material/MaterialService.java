package pl.edu.agh.backend.material;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    @Transactional(readOnly = true)
    public Page<MaterialResponse> list(Optional<MaterialType> type, Optional<UUID> eventId, Pageable pageable) {
        Specification<Material> spec = Specification.allOf(
                MaterialSpecifications.hasType(type.orElse(null)),
                MaterialSpecifications.hasEventId(eventId.orElse(null)));
        return materialRepository.findAll(spec, pageable).map(MaterialResponse::from);
    }
}
