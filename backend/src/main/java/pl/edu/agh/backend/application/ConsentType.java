package pl.edu.agh.backend.application;

import java.util.Set;

public enum ConsentType {
    REGULATIONS_PRIVACY,
    GDPR_DATA_PROCESSING;

    public static final Set<ConsentType> REQUIRED = Set.of(REGULATIONS_PRIVACY, GDPR_DATA_PROCESSING);
}
