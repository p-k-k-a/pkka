// Mock alumni-profile data for the Profil tab. Temporary stand-in until the real
// backend endpoint exists — swap MOCK_ALUMN_PROFILE for a fetched value then.

export type AlumnTag = {
  id: string;
  name: string;
};

/**
 * Visibility toggles only for account-derived fields the user can't hide by
 * leaving them blank (name/email from Keycloak, discordId from the Discord
 * federated identity). Every user-editable field is presence-based: clear it to hide it.
 */
export type AlumnProfileVisibility = {
  name: boolean;
  email: boolean;
  discord: boolean;
};

export type AlumnProfile = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  currentPosition?: string;
  company?: string;
  bio?: string;
  /** Discord snowflake — comes from the Keycloak `federated_identity` link, not user input. */
  discordId?: string;
  linkedinUrl?: string;
  githubUrl?: string;
  graduationYear: number;
  fieldOfStudy: string;
  /** Year the alumn was approved — "alumn od". */
  alumnSince: number;
  tags: AlumnTag[];
  visibility: AlumnProfileVisibility;
};

export const MOCK_ALUMN_PROFILE: AlumnProfile = {
  id: "mock-alumn-1",
  firstName: "Tomasz",
  lastName: "Nowak",
  email: "tomasz.nowak@example.com",
  currentPosition: "Senior Java Developer",
  company: "Google",
  bio: "Pasjonat systemów rozproszonych i architektury cloud-native. Absolwent Wydziału Informatyki AGH, który od 6 lat rozwija krytyczne komponenty infrastruktury globalnej. Skupiony na optymalizacji wydajności oraz skalowalności backendu. Mentor w programach studenckich i prelegent na konferencjach technicznych.",
  discordId: "123456789012345678", // fake snowflake for the mock, not a real id — gitleaks:allow
  linkedinUrl: "https://www.linkedin.com/in/tomasz-nowak",
  githubUrl: "https://github.com/tomasz-nowak",
  graduationYear: 2014,
  fieldOfStudy: "Informatyka",
  alumnSince: 2024,
  tags: [
    { id: "1", name: "Spring" },
    { id: "2", name: "AWS" },
    { id: "3", name: "Kubernetes" },
    { id: "4", name: "Go" },
    { id: "5", name: "Microservices" },
    { id: "6", name: "Terraform" },
  ],
  visibility: {
    name: true,
    email: true,
    discord: true,
  },
};
