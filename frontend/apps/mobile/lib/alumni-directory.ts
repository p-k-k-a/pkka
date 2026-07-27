import { MOCK_ALUMNI_DIRECTORY, type AlumnProfile } from "@/lib/alumni-mock";

export const YEAR_MIN = 1970;
// Derived from the current year so the default upper bound never silently hides
// recent graduates once real data replaces the mock.
export const YEAR_MAX = new Date().getFullYear();

export type SortOption = "name-asc" | "grad-desc" | "grad-asc" | "recent";

export const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: "name-asc", label: "Nazwisko (A-Z)" },
  { value: "grad-desc", label: "Rok ukończenia (Najnowsze)" },
  { value: "grad-asc", label: "Rok ukończenia (Najstarsze)" },
  { value: "recent", label: "Ostatnio aktywni" },
];

export const DEFAULT_SORT: SortOption = "name-asc";

export type AlumniFilters = {
  yearRange: [number, number];
  skills: string[];
  companies: string[];
};

export const EMPTY_FILTERS: AlumniFilters = {
  yearRange: [YEAR_MIN, YEAR_MAX],
  skills: [],
  companies: [],
};

// Mock stand-in for a real GET /api/profiles list endpoint. Keeps the same
// { data, isLoading, isError } shape so the swap to a generated hook is local.
export function useAlumniDirectory() {
  return { alumni: MOCK_ALUMNI_DIRECTORY, isLoading: false, isError: false };
}

export function getAlumnById(id: string): AlumnProfile | undefined {
  return MOCK_ALUMNI_DIRECTORY.find((a) => a.id === id);
}

export function uniqueSkills(alumni: AlumnProfile[]): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const a of alumni) {
    for (const tag of a.tags) {
      if (!seen.has(tag.name)) {
        seen.add(tag.name);
        out.push(tag.name);
      }
    }
  }
  return out.sort((a, b) => a.localeCompare(b, "pl"));
}

export function uniqueCompanies(alumni: AlumnProfile[]): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const a of alumni) {
    if (a.company && !seen.has(a.company)) {
      seen.add(a.company);
      out.push(a.company);
    }
  }
  return out.sort((a, b) => a.localeCompare(b, "pl"));
}

export function filterAlumni(
  alumni: AlumnProfile[],
  query: string,
  filters: AlumniFilters,
): AlumnProfile[] {
  const q = query.trim().toLowerCase();
  const [lowYear, highYear] = filters.yearRange;

  return alumni.filter((a) => {
    const name = `${a.firstName} ${a.lastName}`.toLowerCase();
    const company = (a.company ?? "").toLowerCase();
    const matchesQuery = !q || name.includes(q) || company.includes(q);
    const matchesYear = a.graduationYear >= lowYear && a.graduationYear <= highYear;
    const matchesSkills =
      filters.skills.length === 0 || a.tags.some((tag) => filters.skills.includes(tag.name));
    const matchesCompany =
      filters.companies.length === 0 ||
      (a.company != null && filters.companies.includes(a.company));
    return matchesQuery && matchesYear && matchesSkills && matchesCompany;
  });
}

export function sortAlumni(alumni: AlumnProfile[], sort: SortOption): AlumnProfile[] {
  const copy = [...alumni];
  switch (sort) {
    case "name-asc":
      return copy.sort((a, b) =>
        `${a.lastName} ${a.firstName}`.localeCompare(`${b.lastName} ${b.firstName}`, "pl"),
      );
    case "grad-desc":
      return copy.sort((a, b) => b.graduationYear - a.graduationYear);
    case "grad-asc":
      return copy.sort((a, b) => a.graduationYear - b.graduationYear);
    case "recent":
      return copy.sort((a, b) => (b.lastActiveAt ?? 0) - (a.lastActiveAt ?? 0));
  }
}

// Number of active filter groups, for the badge on the "Filtruj" button. The
// year range counts as one whenever it deviates from the full span.
export function countActiveFilters(filters: AlumniFilters): number {
  let count = filters.skills.length + filters.companies.length;
  if (filters.yearRange[0] !== YEAR_MIN || filters.yearRange[1] !== YEAR_MAX) count += 1;
  return count;
}
