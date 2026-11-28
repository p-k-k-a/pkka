package pl.edu.agh.backend.application;

import java.util.UUID;

/** An admin approved or rejected an application; carries what the mail and push need, read before commit. */
public record ApplicationReviewedEvent(
        UUID applicantId,
        String applicantEmail,
        String applicantFirstName,
        ApplicationStatus decision,
        String rejectionReason) {}
