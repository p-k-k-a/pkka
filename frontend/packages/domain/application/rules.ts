import { CreateApplicationRequestConsentsItem } from "@pkka/api";

/** Matches the backend's `minimum: 1919` on CreateApplicationRequest.graduationYear. */
export const GRADUATION_YEAR_MIN = 1919;

/**
 * The backend sets no upper bound, and the form accepts a *planned* graduation
 * year, so applicants who are still studying can apply. This caps the lookahead
 * at a plausible horizon rather than at today.
 */
export const GRADUATION_YEAR_LOOKAHEAD = 7;

export function graduationYearMax(): number {
  return new Date().getFullYear() + GRADUATION_YEAR_LOOKAHEAD;
}

export function isGraduationYearValid(year: number): boolean {
  return Number.isInteger(year) && year >= GRADUATION_YEAR_MIN && year <= graduationYearMax();
}

export function graduationYearError(): string {
  return `Podaj prawidłowy rok ukończenia (${GRADUATION_YEAR_MIN}–${graduationYearMax()}).`;
}

export const REQUIRED_CONSENTS: string[] = [
  CreateApplicationRequestConsentsItem.REGULATIONS_PRIVACY,
  CreateApplicationRequestConsentsItem.GDPR_DATA_PROCESSING,
];

export function hasRequiredConsents(consents: string[]): boolean {
  return REQUIRED_CONSENTS.every((consent) => consents.includes(consent));
}

export const MISSING_CONSENTS_MESSAGE = "Aby złożyć wniosek, musisz zaakceptować wymagane zgody.";

export const APPLICATION_CONFLICT_MESSAGE =
  "Masz już aktywny wniosek (w trakcie weryfikacji lub zaakceptowany). Nie możesz złożyć kolejnego.";

export const APPLICATION_SUBMIT_ERROR_MESSAGE =
  "Nie udało się wysłać wniosku. Sprawdź dane i spróbuj ponownie.";
