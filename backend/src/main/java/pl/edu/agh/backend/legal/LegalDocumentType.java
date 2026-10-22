package pl.edu.agh.backend.legal;

import io.swagger.v3.oas.annotations.media.Schema;

/** Each type has its own line of versions; consents are documents too, so a record names the exact wording. */
@Schema(enumAsRef = true)
public enum LegalDocumentType {
    TERMS,
    PRIVACY_NOTICE,
    MEMBERSHIP_CONSENT,
    NEWSLETTER_CONSENT
}
