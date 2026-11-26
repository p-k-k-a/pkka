package pl.edu.agh.backend.user;

import java.util.UUID;

/** A user with an address has not been welcomed yet; delivered after the login that noticed it commits. */
public record WelcomeMailDueEvent(UUID userId) {}
