// The backend sends these as `reason` on a 409; the enum is not in the OpenAPI spec yet.
const CONFLICT_MESSAGES: Record<string, string> = {
  ALREADY_REGISTERED: "Jesteś już zapisany na to wydarzenie.",
  REGISTRATION_CLOSED: "Rejestracja na to wydarzenie została zamknięta.",
  EVENT_ALREADY_STARTED: "Wydarzenie już się rozpoczęło.",
};

export type EventRegistrationAction = "join" | "leave";

/**
 * The Polish message for a failed sign-up or cancellation. Reads `status` and `body.reason`
 * structurally, the way `ApiError` carries them, since this package must not import the
 * client's mutator.
 */
export function eventRegistrationErrorMessage(error: unknown, action: EventRegistrationAction) {
  const { status, body } = (error ?? {}) as { status?: unknown; body?: unknown };
  const reason = (body as { reason?: unknown } | null | undefined)?.reason;

  if (status === 401) return "Sesja wygasła. Zaloguj się ponownie.";
  if (typeof reason === "string" && CONFLICT_MESSAGES[reason]) return CONFLICT_MESSAGES[reason];
  if (status === 404) {
    return action === "leave"
      ? "Nie jesteś zapisany na to wydarzenie."
      : "Nie znaleziono wydarzenia.";
  }
  return action === "join"
    ? "Nie udało się zapisać na wydarzenie. Spróbuj ponownie."
    : "Nie udało się wypisać z wydarzenia. Spróbuj ponownie.";
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
