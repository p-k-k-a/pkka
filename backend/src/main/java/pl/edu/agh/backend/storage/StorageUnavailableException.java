package pl.edu.agh.backend.storage;

/** Object storage (R2) is configured but does not answer, so a file cannot be checked. Answered as 503. */
public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(Throwable cause) {
        super("File storage is not available right now", cause);
    }
}
