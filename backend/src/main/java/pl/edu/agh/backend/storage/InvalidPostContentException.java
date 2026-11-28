package pl.edu.agh.backend.storage;

/**
 * Post content that cannot be stored because of a picture in it. Answered as 400 with {@code reason}
 * {@code POST_CONTENT}, so clients can point the admin at the content instead of matching the message.
 */
public class InvalidPostContentException extends InvalidFileException {
    public InvalidPostContentException(String message) {
        super(message);
    }
}
