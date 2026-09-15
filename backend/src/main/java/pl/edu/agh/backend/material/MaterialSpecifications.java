package pl.edu.agh.backend.material;

import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class MaterialSpecifications {

    public Specification<Material> hasType(MaterialType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public Specification<Material> hasEventId(UUID eventId) {
        return (root, query, cb) ->
                eventId == null ? null : cb.equal(root.get("event").get("id"), eventId);
    }
}
