import type {
  ApplicationStatus,
  ConsentType,
  Faculty,
  MeetingPreference,
  StudyType,
} from "@pkka/api";

const FACULTY_LABELS: Record<Faculty, string> = {
  WE: "Wydział Elektromechaniczny (1952-1957)",
  WEGH: "Wydział Elektrotechniki Górniczej i Hutniczej (1957-1975)",
  WEAIE: "Wydział Elektrotechniki, Automatyki i Elektroniki (1975-1998)",
  WEAIIE: "Wydział Elektrotechniki, Automatyki, Informatyki i Elektroniki (1998-2011)",
  WIET: "Wydział Informatyki, Elektroniki i Telekomunikacji (2012-2023)",
  WI: "Wydział Informatyki (2023-obecnie)",
};

const STUDY_TYPE_LABELS: Record<StudyType, string> = {
  BACHELOR: "Studia I stopnia (inżynierskie / licencjackie)",
  MASTER: "Studia II stopnia (magisterskie)",
  DOCTORAL: "Studia doktoranckie",
  POSTGRADUATE: "Studia podyplomowe",
};

const MEETING_PREFERENCE_LABELS: Record<MeetingPreference, string> = {
  ONLINE: "Online",
  IN_PERSON_KRAKOW: "Stacjonarnie (Kraków)",
  HYBRID: "Hybrydowo",
};

const CONSENT_LABELS: Record<ConsentType, string> = {
  REGULATIONS_PRIVACY: "Akceptuję regulamin oraz politykę prywatności",
  GDPR_DATA_PROCESSING: "Wyrażam zgodę na przetwarzanie moich danych osobowych (RODO)",
};

const STATUS_LABELS: Record<ApplicationStatus, string> = {
  UNDER_REVIEW: "W trakcie weryfikacji",
  APPROVED: "Zaakceptowany",
  REJECTED: "Odrzucony",
};

export function facultyLabel(faculty: Faculty): string {
  return FACULTY_LABELS[faculty];
}

export function studyTypeLabel(studyType: StudyType): string {
  return STUDY_TYPE_LABELS[studyType];
}

export function meetingPreferenceLabel(preference: MeetingPreference): string {
  return MEETING_PREFERENCE_LABELS[preference];
}

export function consentLabel(consent: ConsentType): string {
  return CONSENT_LABELS[consent];
}

export function statusLabel(status: ApplicationStatus): string {
  return STATUS_LABELS[status];
}

type LabelOption<T extends string> = { value: T; label: string };

// The request and response enums are distinct generated objects with the same members,
// so each app passes the one its form field is typed against.
export function toOptions<T extends string>(
  values: Record<string, T>,
  label: (value: T) => string,
): LabelOption<T>[] {
  return Object.values(values).map((value) => ({ value, label: label(value) }));
}
