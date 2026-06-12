import {
  ApplicationResponseDtoConsentsItem,
  ApplicationResponseDtoFaculty,
  ApplicationResponseDtoMeetingPreferencesItem,
  ApplicationResponseDtoStudyType,
} from "@pkka/api";

const FACULTY_LABELS: Record<string, string> = {
  WILGZ: "Wydział Inżynierii Lądowej i Gospodarki Zasobami",
  WIMIIP: "Wydział Inżynierii Metali i Informatyki Przemysłowej",
  WEAIIB: "Wydział Elektrotechniki, Automatyki, Informatyki i Inżynierii Biomedycznej",
  WIET: "Wydział Informatyki, Elektroniki i Telekomunikacji",
  WIMIR: "Wydział Inżynierii Mechanicznej i Robotyki",
  WGGIOS: "Wydział Geologii, Geofizyki i Ochrony Środowiska",
  WGGIIS: "Wydział Geodezji Górniczej i Inżynierii Środowiska",
  WIMIC: "Wydział Inżynierii Materiałowej i Ceramiki",
  WO: "Wydział Odlewnictwa",
  WMN: "Wydział Metali Nieżelaznych",
  WWNIG: "Wydział Wiertnictwa, Nafty i Gazu",
  WZ: "Wydział Zarządzania",
  WEIP: "Wydział Energetyki i Paliw",
  WFIIS: "Wydział Fizyki i Informatyki Stosowanej",
  WMS: "Wydział Matematyki Stosowanej",
  WH: "Wydział Humanistyczny",
  WI: "Wydział Informatyki",
  WTK: "Wydział Technologii Kosmicznej",
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
