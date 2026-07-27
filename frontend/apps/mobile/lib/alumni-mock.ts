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
  /** Directory-only metadata: epoch ms of last activity, for the "Ostatnio aktywni" sort. */
  lastActiveAt?: number;
};

const t = (...names: string[]): AlumnTag[] => names.map((name, i) => ({ id: String(i + 1), name }));

const PUBLIC_VISIBILITY: AlumnProfileVisibility = { name: true, email: true, discord: true };

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

// Mock directory of approved alumni for the "Katalog Alumnów" tab. Stands in for
// a real GET /api/profiles list endpoint — swap MOCK_ALUMNI_DIRECTORY for the
// fetched page then (see lib/alumni-directory.ts).
export const MOCK_ALUMNI_DIRECTORY: AlumnProfile[] = [
  {
    id: "alumn-1",
    firstName: "Tomasz",
    lastName: "Nowak",
    email: "tomasz.nowak@example.com",
    currentPosition: "Senior Java Developer",
    company: "Google",
    bio: "Pasjonat systemów rozproszonych i architektury cloud-native. Od 6 lat rozwija krytyczne komponenty infrastruktury globalnej.",
    discordId: "123456789012345678", // fake snowflake for the mock — gitleaks:allow
    linkedinUrl: "https://www.linkedin.com/in/tomasz-nowak",
    githubUrl: "https://github.com/tomasz-nowak",
    graduationYear: 2014,
    fieldOfStudy: "Informatyka",
    alumnSince: 2024,
    tags: t("Spring", "AWS", "Kubernetes", "Go"),
    visibility: PUBLIC_VISIBILITY,
    lastActiveAt: 1_721_300_000_000,
  },
  {
    id: "alumn-2",
    firstName: "Aleksandra",
    lastName: "Wiśniewska",
    email: "aleksandra.wisniewska@example.com",
    currentPosition: "Data Scientist",
    company: "NVIDIA",
    bio: "Buduje modele uczenia maszynowego dla systemów wizyjnych. Mentorka w programach dla studentek kierunków technicznych.",
    linkedinUrl: "https://www.linkedin.com/in/aleksandra-wisniewska",
    graduationYear: 2018,
    fieldOfStudy: "Informatyka",
    alumnSince: 2023,
    tags: t("Python", "PyTorch", "Mentor"),
    visibility: PUBLIC_VISIBILITY,
    lastActiveAt: 1_721_100_000_000,
  },
  {
    id: "alumn-3",
    firstName: "Marek",
    lastName: "Kowalski",
    email: "marek.kowalski@example.com",
    currentPosition: "Product Manager",
    company: "Allegro",
    graduationYear: 2016,
    fieldOfStudy: "Informatyka i Ekonometria",
    alumnSince: 2024,
    tags: t("Agile", "UI/UX"),
    visibility: PUBLIC_VISIBILITY,
    lastActiveAt: 1_720_900_000_000,
  },
  {
    id: "alumn-4",
    firstName: "Katarzyna",
    lastName: "Lewandowska",
    email: "katarzyna.lewandowska@example.com",
    currentPosition: "DevOps Engineer",
    company: "Comarch",
    bio: "Automatyzuje pipeline'y CI/CD i infrastrukturę chmurową dla zespołów produktowych.",
    githubUrl: "https://github.com/katarzyna-lewandowska",
    graduationYear: 2019,
    fieldOfStudy: "Informatyka",
    alumnSince: 2023,
    tags: t("Docker", "AWS", "Terraform", "DevOps"),
    visibility: PUBLIC_VISIBILITY,
    lastActiveAt: 1_720_700_000_000,
  },
  {
    id: "alumn-5",
    firstName: "Piotr",
    lastName: "Zieliński",
    email: "piotr.zielinski@example.com",
    currentPosition: "Frontend Developer",
    company: "ABB",
    linkedinUrl: "https://www.linkedin.com/in/piotr-zielinski",
    githubUrl: "https://github.com/piotr-zielinski",
    graduationYear: 2021,
    fieldOfStudy: "Informatyka",
    alumnSince: 2024,
    tags: t("React", "TypeScript", "UI/UX"),
    visibility: PUBLIC_VISIBILITY,
    lastActiveAt: 1_720_500_000_000,
  },
  {
    id: "alumn-6",
    firstName: "Magdalena",
    lastName: "Wójcik",
    email: "magdalena.wojcik@example.com",
    currentPosition: "Machine Learning Engineer",
    company: "Google",
    bio: "Wdraża modele ML na produkcję w skali globalnej. Prelegentka na konferencjach o MLOps.",
    graduationYear: 2015,
    fieldOfStudy: "Informatyka",
    alumnSince: 2022,
    tags: t("Python", "AWS", "DevOps"),
    visibility: PUBLIC_VISIBILITY,
    lastActiveAt: 1_720_300_000_000,
  },
  {
    id: "alumn-7",
    firstName: "Jakub",
    lastName: "Szymański",
    email: "jakub.szymanski@example.com",
    currentPosition: "Backend Developer",
    company: "Comarch",
    graduationYear: 2012,
    fieldOfStudy: "Informatyka",
    alumnSince: 2023,
    tags: t("Java", "Spring", "Microservices"),
    visibility: PUBLIC_VISIBILITY,
    lastActiveAt: 1_720_100_000_000,
  },
];
