package pl.edu.agh.backend.legal;

public enum ConsentSource {
    APPLICATION,
    REACCEPTANCE,
    SETTINGS,
    /** Migrated from the application form's consents, given before documents were versioned. */
    LEGACY
}
