package pl.edu.agh.backend.topic;

public class TopicProposalNotFoundException extends RuntimeException {

    public TopicProposalNotFoundException() {
        super("Topic proposal not found");
    }
}
