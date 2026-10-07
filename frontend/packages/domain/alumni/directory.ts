import type { ListAlumniParams } from "@pkka/api";

export const ALUMNI_YEAR_MIN = 1970;
export const ALUMNI_YEAR_MAX = new Date().getFullYear();

export const ALUMNI_PAGE_SIZE = 20;

export type AlumniSortOption = "name-asc" | "name-desc" | "grad-desc" | "grad-asc";

export const ALUMNI_SORT_OPTIONS: { value: AlumniSortOption; label: string }[] = [
  { value: "name-asc", label: "Nazwisko (A-Z)" },
  { value: "name-desc", label: "Nazwisko (Z-A)" },
  { value: "grad-desc", label: "Rok ukończenia (Najnowsze)" },
  { value: "grad-asc", label: "Rok ukończenia (Najstarsze)" },
];

// Spring Data `sort` values; the properties are columns of `users`, so both
// lastName and graduationYear are sortable server-side.
const SORT_PARAM: Record<AlumniSortOption, string> = {
  "name-asc": "lastName,asc",
  "name-desc": "lastName,desc",
  "grad-desc": "graduationYear,desc",
  "grad-asc": "graduationYear,asc",
};

export const DEFAULT_ALUMNI_SORT: AlumniSortOption = "name-asc";

export type AlumniFilters = {
  yearRange: [number, number];
  tagIds: string[];
  mentorOnly: boolean;
};

export const EMPTY_ALUMNI_FILTERS: AlumniFilters = {
  yearRange: [ALUMNI_YEAR_MIN, ALUMNI_YEAR_MAX],
  tagIds: [],
  mentorOnly: false,
};

export function buildAlumniParams(
  query: string,
  filters: AlumniFilters,
  sort: AlumniSortOption,
): ListAlumniParams {
  const trimmedQuery = query.trim();
  const [lowYear, highYear] = filters.yearRange;

  return {
    size: ALUMNI_PAGE_SIZE,
    sort: [SORT_PARAM[sort]],
    ...(trimmedQuery ? { q: trimmedQuery } : {}),
    ...(filters.tagIds.length > 0 ? { tagIds: filters.tagIds } : {}),
    ...(filters.mentorOnly ? { mentor: true } : {}),
    ...(lowYear > ALUMNI_YEAR_MIN ? { graduationYearFrom: lowYear } : {}),
    ...(highYear < ALUMNI_YEAR_MAX ? { graduationYearTo: highYear } : {}),
  };
}

export function countActiveAlumniFilters(filters: AlumniFilters): number {
  let count = filters.tagIds.length;
  if (filters.mentorOnly) count += 1;
  if (filters.yearRange[0] !== ALUMNI_YEAR_MIN || filters.yearRange[1] !== ALUMNI_YEAR_MAX) {
    count += 1;
  }
  return count;
}
