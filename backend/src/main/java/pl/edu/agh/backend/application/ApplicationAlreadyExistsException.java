package pl.edu.agh.backend.application;

public class ApplicationAlreadyExistsException extends RuntimeException {

    public ApplicationAlreadyExistsException() {
        super("User already has an application under review");
    }
}
