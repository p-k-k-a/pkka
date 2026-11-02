package pl.edu.agh.backend.storage;

/** Object storage (R2) is not configured, so nothing can be signed. Answered as 503. */
public class StorageNotConfiguredException extends RuntimeException {
    public StorageNotConfiguredException() {
        super("File storage is not configured");
    }
}
