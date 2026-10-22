package pl.edu.agh.backend.legal;

import java.util.Set;
import lombok.Getter;

@Getter
public class MissingRequiredConsentException extends RuntimeException {

    private final Set<LegalDocumentType> missing;

    public MissingRequiredConsentException(Set<LegalDocumentType> missing) {
        super("Required documents not accepted: %s".formatted(missing));
        this.missing = Set.copyOf(missing);
    }
}
