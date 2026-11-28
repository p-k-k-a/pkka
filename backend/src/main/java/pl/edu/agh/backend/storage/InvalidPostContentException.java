package pl.edu.agh.backend.storage;

/**
 * Post content that cannot be stored. Answered as 400 with the {@link Reason} as {@code reason}, so clients can
 * tell the admin what to fix instead of matching the message.
 */
public class InvalidPostContentException extends InvalidFileException {

    public enum Reason {
        /**
         * A link to a stored file other than a post picture (a material, a cover, an avatar). Only pictures uploaded
         * for a post are signed for its readers; signing anything else would hand that file to everyone who can
         * read the post.
         */
        POST_CONTENT_LINK,
        /** A picture that never reached the storage, e.g. a {@code blob:} preview pasted from another editor. */
        POST_CONTENT_PICTURE
    }

    private final Reason reason;

    public InvalidPostContentException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
