import type { ConsentType } from "@pkka/api";

export const GRADUATION_YEAR_MIN = 1919;

// The form asks for the graduation year "or the planned one", so upcoming years are valid.
// The backend only constrains @Min(1919); this cap is a client-side sanity check.
const GRADUATION_YEAR_LOOKAHEAD = 7;

export function graduationYearMax(): number {
  return new Date().getFullYear() + GRADUATION_YEAR_LOOKAHEAD;
}

export function isGraduationYearValid(year: number): boolean {
  return Number.isInteger(year) && year >= GRADUATION_YEAR_MIN && year <= graduationYearMax();
}

export function graduationYearError(): string {
  return `Podaj rok ukończenia między ${GRADUATION_YEAR_MIN} a ${graduationYearMax()}.`;
}

const REQUIRED_CONSENTS: ConsentType[] = ["REGULATIONS_PRIVACY", "GDPR_DATA_PROCESSING"];

export function hasRequiredConsents(consents: readonly ConsentType[]): boolean {
  return REQUIRED_CONSENTS.every((consent) => consents.includes(consent));
}

export const APPLICATION_CONFLICT_MESSAGE =
  "Masz już aktywny wniosek (w trakcie weryfikacji lub zaakceptowany). Nie możesz złożyć kolejnego.";

export const APPLICATION_SUBMIT_ERROR_MESSAGE =
  "Nie udało się wysłać wniosku. Sprawdź dane i spróbuj ponownie.";

// Offered by the application form on both platforms. Kept a closed list so the same
// interest is comparable across submissions; the backend field itself is free-form.
export const INTEREST_AREAS = [
  "Technologie przyszłości i przełomowe innowacje",
  "Kariera, przywództwo i rozwój osobisty",
  "Przedsiębiorczość i innowacje",
  "Technologie a odpowiedzialność",
  "Wsparcie dla Wydziału i studentów",
  "Relacje i wspólnota",
] as const;

// Linked from the consent step. Placeholders until the real documents are published.
export const TERMS_URL = "https://example.com/regulamin";
export const PRIVACY_URL = "https://example.com/polityka-prywatnosci";
