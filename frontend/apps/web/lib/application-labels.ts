import {
  ApplicationResponseDtoConsentsItem,
  ApplicationResponseDtoFaculty,
  ApplicationResponseDtoMeetingPreferencesItem,
  ApplicationResponseDtoStudyType,
} from "@pkka/api";

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

export const FACULTY_OPTIONS = Object.values(ApplicationResponseDtoFaculty).map((value) => ({
  value,
  label: facultyLabel(value),
}));

export const STUDY_TYPE_OPTIONS = Object.values(ApplicationResponseDtoStudyType).map((value) => ({
  value,
  label: studyTypeLabel(value),
}));

export const MEETING_PREFERENCE_OPTIONS = Object.values(
  ApplicationResponseDtoMeetingPreferencesItem,
).map((value) => ({
  value,
  label: meetingPreferenceLabel(value),
}));

export const CONSENT_OPTIONS = Object.values(ApplicationResponseDtoConsentsItem).map((value) => ({
  value,
  label: consentLabel(value),
}));
