package pl.edu.agh.backend.post;

public class PostAlreadyPublishedException extends RuntimeException {

    public PostAlreadyPublishedException() {
        super("A published post cannot be reverted to a draft");
    }
}
