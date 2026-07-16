package pl.edu.agh.backend.application;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException() {
        super("No application found for the current user");
    }
}
