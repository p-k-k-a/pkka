package pl.edu.agh.backend.alumni;

import java.util.UUID;

public class AlumniNotFoundException extends RuntimeException {
    public AlumniNotFoundException(UUID id) {
        super("Alumni with id %s not found".formatted(id));
    }
}
