import {
  type CreateApplicationRequestDtoFaculty,
  type CreateApplicationRequestDtoMeetingPreferencesItem,
  type CreateApplicationRequestDtoStudyType,
} from "@pkka/api";

export const FACULTIES: { value: CreateApplicationRequestDtoFaculty; label: string }[] = [
  { value: "WILGZ", label: "Wydział Inżynierii Lądowej i Gospodarki Zasobami" },
  { value: "WIMIIP", label: "Wydział Inżynierii Metali i Informatyki Przemysłowej" },
  {
    value: "WEAIIB",
    label: "Wydział Elektrotechniki, Automatyki, Informatyki i Inżynierii Biomedycznej",
  },
  { value: "WIET", label: "Wydział Informatyki, Elektroniki i Telekomunikacji" },
  { value: "WIMIR", label: "Wydział Inżynierii Mechanicznej i Robotyki" },
  { value: "WGGIOS", label: "Wydział Geologii, Geofizyki i Ochrony Środowiska" },
  { value: "WGGIIS", label: "Wydział Geodezji Górniczej i Inżynierii Środowiska" },
  { value: "WIMIC", label: "Wydział Inżynierii Materiałowej i Ceramiki" },
  { value: "WO", label: "Wydział Odlewnictwa" },
  { value: "WMN", label: "Wydział Metali Nieżelaznych" },
  { value: "WWNIG", label: "Wydział Wiertnictwa, Nafty i Gazu" },
  { value: "WZ", label: "Wydział Zarządzania" },
  { value: "WEIP", label: "Wydział Energetyki i Paliw" },
  { value: "WFIIS", label: "Wydział Fizyki i Informatyki Stosowanej" },
  { value: "WMS", label: "Wydział Matematyki Stosowanej" },
  { value: "WH", label: "Wydział Humanistyczny" },
  { value: "WI", label: "Wydział Informatyki" },
  { value: "WTK", label: "Wydział Technologii Kosmicznych" },
];

export const STUDY_TYPES: { value: CreateApplicationRequestDtoStudyType; label: string }[] = [
  { value: "BACHELOR", label: "Studia licencjackie / inżynierskie" },
  { value: "MASTER", label: "Studia magisterskie" },
  { value: "DOCTORAL", label: "Studia doktoranckie" },
  { value: "POSTGRADUATE", label: "Studia podyplomowe" },
];

export const INTEREST_AREAS = [
  "Technologie przyszłości i przełomowe innowacje",
  "Kariera, przywództwo i rozwój osobisty",
  "Przedsiębiorczość i innowacje",
  "Technologie a odpowiedzialność",
  "Wsparcie dla Wydziału i studentów",
  "Relacje i wspólnota",
] as const;

export const MEETING_FORMATS: {
  value: CreateApplicationRequestDtoMeetingPreferencesItem;
  label: string;
}[] = [
  { value: "ONLINE", label: "Online" },
  { value: "IN_PERSON_KRAKOW", label: "Na żywo (Kraków)" },
  { value: "HYBRID", label: "Hybrydowo" },
];

export const TERMS_URL = "https://example.com/regulamin";
export const PRIVACY_URL = "https://example.com/polityka-prywatnosci";
