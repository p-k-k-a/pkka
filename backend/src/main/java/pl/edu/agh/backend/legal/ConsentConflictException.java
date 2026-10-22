package pl.edu.agh.backend.legal;

import java.util.UUID;
import lombok.Getter;

@Getter
public class ConsentConflictException extends RuntimeException {

    public enum Reason {
        /** The document is unknown or a newer version is in force; the client should reload the documents. */
        OUTDATED_DOCUMENT,
        NOT_WITHDRAWABLE
    }

    private final Reason reason;

    private ConsentConflictException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public static ConsentConflictException outdatedDocument(UUID documentId) {
        return new ConsentConflictException(
                Reason.OUTDATED_DOCUMENT, "Document %s is not a version currently in force".formatted(documentId));
    }

    public static ConsentConflictException notWithdrawable(LegalDocumentType type) {
        return new ConsentConflictException(
                Reason.NOT_WITHDRAWABLE, "Consent %s cannot be withdrawn in the app".formatted(type));
    }
}
