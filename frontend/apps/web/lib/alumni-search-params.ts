import {
  ALUMNI_SORT_OPTIONS,
  ALUMNI_YEAR_MAX,
  ALUMNI_YEAR_MIN,
  DEFAULT_ALUMNI_SORT,
  type AlumniFilters,
  type AlumniSortOption,
} from "@pkka/domain";

export const ALUMNI_DIRECTORY_HREF = "/dashboard/alumni";

export type AlumniDirectoryState = {
  query: string;
  filters: AlumniFilters;
  sort: AlumniSortOption;
  /** Zero-based, like the API; the URL shows it one-based. */
  page: number;
};

type ReadableSearchParams = { get(name: string): string | null };

function parseYear(value: string | null, fallback: number) {
  const year = Number(value);
  return value && Number.isInteger(year) && year >= ALUMNI_YEAR_MIN && year <= ALUMNI_YEAR_MAX
    ? year
    : fallback;
}

// The URL can be edited by hand or shared, so anything out of range falls back to
// the default instead of reaching the API.
export function parseAlumniSearchParams(params: ReadableSearchParams): AlumniDirectoryState {
  const sort = params.get("sort");
  const from = parseYear(params.get("from"), ALUMNI_YEAR_MIN);
  const to = parseYear(params.get("to"), ALUMNI_YEAR_MAX);
  const page = Number(params.get("page"));

  return {
    query: params.get("q") ?? "",
    sort: ALUMNI_SORT_OPTIONS.some((option) => option.value === sort)
      ? (sort as AlumniSortOption)
      : DEFAULT_ALUMNI_SORT,
    filters: {
      yearRange: from <= to ? [from, to] : [ALUMNI_YEAR_MIN, ALUMNI_YEAR_MAX],
      tagIds: params.get("tags")?.split(",").filter(Boolean) ?? [],
      mentorOnly: params.get("mentor") === "1",
    },
    page: Number.isInteger(page) && page > 1 ? page - 1 : 0,
  };
}

/** `?…` for the given state, or "" when everything is at its default. */
export function toAlumniSearchString({ query, filters, sort, page }: AlumniDirectoryState) {
  const params = new URLSearchParams();
  const trimmedQuery = query.trim();
  const [from, to] = filters.yearRange;

  if (trimmedQuery) params.set("q", trimmedQuery);
  if (sort !== DEFAULT_ALUMNI_SORT) params.set("sort", sort);
  if (from > ALUMNI_YEAR_MIN) params.set("from", String(from));
  if (to < ALUMNI_YEAR_MAX) params.set("to", String(to));
  if (filters.tagIds.length > 0) params.set("tags", filters.tagIds.join(","));
  if (filters.mentorOnly) params.set("mentor", "1");
  if (page > 0) params.set("page", String(page + 1));

  const search = params.toString();
  return search ? `?${search}` : "";
}
