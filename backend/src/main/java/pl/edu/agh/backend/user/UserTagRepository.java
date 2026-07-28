package pl.edu.agh.backend.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTagRepository extends JpaRepository<UserTag, UUID> {}
