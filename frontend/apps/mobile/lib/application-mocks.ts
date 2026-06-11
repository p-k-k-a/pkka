/**
 * TEMPORARY MOCK DATA — membership application prototype.
 *
 * Everything the application flow needs that will eventually come from the
 * backend lives in this single file. Once the /api/applications endpoints
 * are exposed through orval (@pkka/api), delete this file and swap the
 * imports for the generated types and hooks.
 */

export type StudyTypeValue = "BACHELOR" | "MASTER" | "DOCTORAL" | "POSTGRADUATE";

export type ApplicationFormData = {
  phone: string;
  faculty: string;
  fieldOfStudy: string;
  studyType: StudyTypeValue;
  graduationYear: number;
  interests: string[];
  meetingFormat: string[];
  wantsToContribute: boolean;
  newsletter: boolean;
};

export const MOCK_ACCOUNT_STATUS = {
  verified: false,
  label: "Status: Niezweryfikowany",
} as const;

export const FACULTIES = [
  "Wydział Informatyki",
  "Wydział Elektrotechniki, Automatyki, Informatyki i Inżynierii Biomedycznej",
  "Wydział Informatyki, Elektroniki i Telekomunikacji",
  "Wydział Matematyki Stosowanej",
  "Wydział Fizyki i Informatyki Stosowanej",
] as const;

export const STUDY_TYPES: { value: StudyTypeValue; label: string }[] = [
  { value: "BACHELOR", label: "Inżynierskie / licencjackie" },
  { value: "MASTER", label: "Magisterskie" },
  { value: "DOCTORAL", label: "Doktoranckie" },
  { value: "POSTGRADUATE", label: "Podyplomowe" },
];

export const INTEREST_AREAS = [
  "Przyszłość technologii",
  "Rozwój kariery / Mentoring",
  "Networking",
  "Współpraca naukowa",
] as const;

export const MEETING_FORMATS = ["Online", "Na żywo (Kraków)", "Hybrydowo"] as const;

export const TERMS_URL = "https://example.com/regulamin";
export const PRIVACY_URL = "https://example.com/polityka-prywatnosci";

/** Pretends to POST the application; resolves after a short delay. */
export function mockSubmitApplication(data: ApplicationFormData): Promise<void> {
  console.log("[mock] submitting application", data);
  return new Promise((resolve) => setTimeout(resolve, 800));
}

/** Placeholder for the Discord verification flow — not implemented yet. */
export function mockVerifyWithDiscord(): void {
  console.log("[mock] discord verification is not wired up yet");
}
