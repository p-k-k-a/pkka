package pl.edu.agh.backend.legal;

public class LegalDocumentNotFoundException extends RuntimeException {

    public LegalDocumentNotFoundException(LegalDocumentType type) {
        super("No published version of %s".formatted(type));
    }
}
