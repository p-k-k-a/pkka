package pl.edu.agh.backend.application;

import java.util.UUID;

public class InvalidApplicationStateException extends RuntimeException {

    public InvalidApplicationStateException(UUID applicationId, ApplicationStatus current) {
        super("Application %s cannot change state — already %s".formatted(applicationId, current));
    }
}
