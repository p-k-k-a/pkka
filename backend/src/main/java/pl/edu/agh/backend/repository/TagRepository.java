package pl.edu.agh.backend.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.edu.agh.backend.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, UUID> {}
