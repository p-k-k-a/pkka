package pl.edu.agh.backend.application;

/** An admin approved or rejected an application; carries what the decision mail needs, read before commit. */
public record ApplicationReviewedEvent(
        String applicantEmail, String applicantFirstName, ApplicationStatus decision, String rejectionReason) {}
