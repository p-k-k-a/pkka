import {
  CreateApplicationRequestDtoFaculty,
  CreateApplicationRequestDtoMeetingPreferencesItem,
  CreateApplicationRequestDtoStudyType,
} from "@pkka/api";

type Option<T extends string> = { value: T; label: string };

const FACULTY_LABELS: Record<CreateApplicationRequestDtoFaculty, string> = {
  WE: "Wydział Elektromechaniczny (1952-1957)",
  WEGH: "Wydział Elektrotechniki Górniczej i Hutniczej (1957-1975)",
  WEAIE: "Wydział Elektrotechniki, Automatyki i Elektroniki (1975-1998)",
  WEAIIE: "Wydział Elektrotechniki, Automatyki, Informatyki i Elektroniki (1998-2011)",
  WIET: "Wydział Informatyki, Elektroniki i Telekomunikacji (2012-2023)",
  WI: "Wydział Informatyki (2023-obecnie)",
};

const STUDY_TYPE_LABELS: Record<CreateApplicationRequestDtoStudyType, string> = {
  BACHELOR: "Studia I stopnia (inżynierskie / licencjackie)",
  MASTER: "Studia II stopnia (magisterskie)",
  DOCTORAL: "Studia doktoranckie",
  POSTGRADUATE: "Studia podyplomowe",
};

const MEETING_FORMAT_LABELS: Record<CreateApplicationRequestDtoMeetingPreferencesItem, string> = {
  ONLINE: "Online",
  IN_PERSON_KRAKOW: "Stacjonarnie (Kraków)",
  HYBRID: "Hybrydowo",
};

export const FACULTIES: Option<CreateApplicationRequestDtoFaculty>[] = Object.values(
  CreateApplicationRequestDtoFaculty,
).map((value) => ({ value, label: FACULTY_LABELS[value] }));

export const STUDY_TYPES: Option<CreateApplicationRequestDtoStudyType>[] = Object.values(
  CreateApplicationRequestDtoStudyType,
).map((value) => ({ value, label: STUDY_TYPE_LABELS[value] }));

export const MEETING_FORMATS: Option<CreateApplicationRequestDtoMeetingPreferencesItem>[] =
  Object.values(CreateApplicationRequestDtoMeetingPreferencesItem).map((value) => ({
    value,
    label: MEETING_FORMAT_LABELS[value],
  }));

export const INTEREST_AREAS = [
  "Technologie przyszłości i przełomowe innowacje",
  "Kariera, przywództwo i rozwój osobisty",
  "Przedsiębiorczość i innowacje",
  "Technologie a odpowiedzialność",
  "Wsparcie dla Wydziału i studentów",
  "Relacje i wspólnota",
] as const;

export const TERMS_URL = "https://example.com/regulamin";
export const PRIVACY_URL = "https://example.com/polityka-prywatnosci";
