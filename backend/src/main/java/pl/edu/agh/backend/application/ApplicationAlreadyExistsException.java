package pl.edu.agh.backend.application;

import java.util.UUID;

public class ApplicationAlreadyExistsException extends RuntimeException {

    public ApplicationAlreadyExistsException(UUID applicantId) {
        super("User %s already has an application under review".formatted(applicantId));
    }
}
