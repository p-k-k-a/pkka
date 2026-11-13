package pl.edu.agh.backend.storage;

/** The content type is not allowed for this kind of upload. Answered as 415. */
public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException(String contentType) {
        super("Content type is not allowed here: " + contentType);
    }
}
