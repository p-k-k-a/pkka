package pl.edu.agh.backend.storage;

/** The file exceeds the size limit of its upload category. Answered as 413. */
public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(long maxBytes) {
        super("File is larger than the " + maxBytes + " byte limit");
    }
}
