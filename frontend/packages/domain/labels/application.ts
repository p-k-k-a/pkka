const FACULTY_LABELS: Record<string, string> = {
  WE: "Wydział Elektromechaniczny (1952-1957)",
  WEGH: "Wydział Elektrotechniki Górniczej i Hutniczej (1957-1975)",
  WEAIE: "Wydział Elektrotechniki, Automatyki i Elektroniki (1975-1998)",
  WEAIIE: "Wydział Elektrotechniki, Automatyki, Informatyki i Elektroniki (1998-2011)",
  WIET: "Wydział Informatyki, Elektroniki i Telekomunikacji (2012-2023)",
  WI: "Wydział Informatyki (2023-obecnie)",
};

const STUDY_TYPE_LABELS: Record<string, string> = {
  BACHELOR: "Studia I stopnia (inżynierskie / licencjackie)",
  MASTER: "Studia II stopnia (magisterskie)",
  DOCTORAL: "Studia doktoranckie",
  POSTGRADUATE: "Studia podyplomowe",
};

const MEETING_PREFERENCE_LABELS: Record<string, string> = {
  ONLINE: "Online",
  IN_PERSON_KRAKOW: "Stacjonarnie (Kraków)",
  HYBRID: "Hybrydowo",
};

const CONSENT_LABELS: Record<string, string> = {
  REGULATIONS_PRIVACY: "Akceptuję regulamin oraz politykę prywatności",
  GDPR_DATA_PROCESSING: "Wyrażam zgodę na przetwarzanie moich danych osobowych (RODO)",
};

const STATUS_LABELS: Record<string, string> = {
  UNDER_REVIEW: "W trakcie weryfikacji",
  APPROVED: "Zaakceptowany",
  REJECTED: "Odrzucony",
};

export function facultyLabel(faculty: string): string {
  return FACULTY_LABELS[faculty] ?? faculty;
}

export function studyTypeLabel(studyType: string): string {
  return STUDY_TYPE_LABELS[studyType] ?? studyType;
}

export function meetingPreferenceLabel(preference: string): string {
  return MEETING_PREFERENCE_LABELS[preference] ?? preference;
}

export function consentLabel(consent: string): string {
  return CONSENT_LABELS[consent] ?? consent;
}

export function statusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status;
}

export type LabelOption<T extends string> = { value: T; label: string };

/**
 * Builds picker options from a generated enum object. The request and response
 * enums (`CreateApplicationRequestFaculty` / `ApplicationResponseFaculty`) are
 * distinct objects with identical members, so each app passes its own.
 */
export function toOptions<T extends string>(
  values: Record<string, T>,
  label: (value: string) => string,
): LabelOption<T>[] {
  return Object.values(values).map((value) => ({ value, label: label(value) }));
}
