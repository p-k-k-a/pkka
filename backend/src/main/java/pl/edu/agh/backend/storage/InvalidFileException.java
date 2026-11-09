package pl.edu.agh.backend.storage;

/** A file reference or upload request the client got wrong (bad key, wrong purpose, empty file). Answered as 400. */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
