package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(enumAsRef = true)
public enum ConsentType {
    REGULATIONS_PRIVACY,
    GDPR_DATA_PROCESSING;

    public static final Set<ConsentType> REQUIRED = Set.of(REGULATIONS_PRIVACY, GDPR_DATA_PROCESSING);
}
