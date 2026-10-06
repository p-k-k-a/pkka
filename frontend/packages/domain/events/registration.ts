// The backend sends these as `reason` on a 409; the enum is not in the OpenAPI spec yet.
const CONFLICT_MESSAGES: Record<string, string> = {
  ALREADY_REGISTERED: "Jesteś już zapisany na to wydarzenie.",
  REGISTRATION_CLOSED: "Rejestracja na to wydarzenie została zamknięta.",
  EVENT_ALREADY_STARTED: "Wydarzenie już się rozpoczęło.",
};

/** The Polish message for a 409 `reason`, or null when the reason is unknown. */
export function eventRegistrationConflictMessage(reason: unknown) {
  return typeof reason === "string" ? (CONFLICT_MESSAGES[reason] ?? null) : null;
}

/** Mirrors the backend: closed once the event has started or `registrationClosesAt` has passed. */
export function isEventRegistrationClosed(
  event: { startsAt: string; registrationClosesAt?: string },
  now: number = Date.now(),
) {
  const startsAt = Date.parse(event.startsAt);
  const closesAt = event.registrationClosesAt ? Date.parse(event.registrationClosesAt) : NaN;
  return now >= startsAt || (!Number.isNaN(closesAt) && now >= closesAt);
}

/** Only held seats count, so a full event still takes sign-ups — onto the waitlist. */
export function isEventFull(seatLimit?: number, seatsTaken?: number) {
  return seatLimit != null && (seatsTaken ?? 0) >= seatLimit;
}
