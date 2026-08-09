import { useDebouncedValue } from "@/lib/use-debounced-value";
import {
  useListAlumniInfinite,
  type AlumniListItemResponse,
  type ListAlumniParams,
} from "@pkka/api";
import { useMemo } from "react";

export const YEAR_MIN = 1970;
export const YEAR_MAX = new Date().getFullYear();

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

export type SortOption = "name-asc" | "name-desc" | "grad-desc" | "grad-asc";

export const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: "name-asc", label: "Nazwisko (A-Z)" },
  { value: "name-desc", label: "Nazwisko (Z-A)" },
  { value: "grad-desc", label: "Rok ukończenia (Najnowsze)" },
  { value: "grad-asc", label: "Rok ukończenia (Najstarsze)" },
];

// Spring Data `sort` values; the properties are columns of `users`, so both
// lastName and graduationYear are sortable server-side.
const SORT_PARAM: Record<SortOption, string> = {
  "name-asc": "lastName,asc",
  "name-desc": "lastName,desc",
  "grad-desc": "graduationYear,desc",
  "grad-asc": "graduationYear,asc",
};

export const DEFAULT_SORT: SortOption = "name-asc";

export type AlumniFilters = {
  yearRange: [number, number];
  tagIds: string[];
  mentorOnly: boolean;
};

export const EMPTY_FILTERS: AlumniFilters = {
  yearRange: [YEAR_MIN, YEAR_MAX],
  tagIds: [],
  mentorOnly: false,
};

export function buildAlumniParams(
  query: string,
  filters: AlumniFilters,
  sort: SortOption,
): ListAlumniParams {
  const trimmedQuery = query.trim();
  const [lowYear, highYear] = filters.yearRange;

  return {
    size: PAGE_SIZE,
    sort: [SORT_PARAM[sort]],
    ...(trimmedQuery ? { q: trimmedQuery } : {}),
    ...(filters.tagIds.length > 0 ? { tagIds: filters.tagIds } : {}),
    ...(filters.mentorOnly ? { mentor: true } : {}),
    ...(lowYear > YEAR_MIN ? { graduationYearFrom: lowYear } : {}),
    ...(highYear < YEAR_MAX ? { graduationYearTo: highYear } : {}),
  };
}

type UseAlumniDirectoryArgs = {
  query: string;
  filters: AlumniFilters;
  sort: SortOption;
};

export function useAlumniDirectory({ query, filters, sort }: UseAlumniDirectoryArgs) {
  const debouncedQuery = useDebouncedValue(query, SEARCH_DEBOUNCE_MS);
  const params = buildAlumniParams(debouncedQuery, filters, sort);

  const result = useListAlumniInfinite(params, {
    query: {
      initialPageParam: 0,
      getNextPageParam: (lastPage) => {
        if (lastPage.status !== 200) return undefined;
        const page = lastPage.data;
        const next = (page.number ?? 0) + 1;
        return next < (page.totalPages ?? 0) ? next : undefined;
      },
    },
  });

  const alumni = useMemo<AlumniListItemResponse[]>(
    () =>
      result.data?.pages.flatMap((page) =>
        page.status === 200 ? (page.data.content ?? []) : [],
      ) ?? [],
    [result.data],
  );

  return {
    alumni,
    isLoading: result.isPending,
    isError: result.isError,
    refetch: result.refetch,
    fetchNextPage: result.fetchNextPage,
    hasNextPage: result.hasNextPage,
    isFetchingNextPage: result.isFetchingNextPage,
  };
}

export function countActiveFilters(filters: AlumniFilters): number {
  let count = filters.tagIds.length;
  if (filters.mentorOnly) count += 1;
  if (filters.yearRange[0] !== YEAR_MIN || filters.yearRange[1] !== YEAR_MAX) count += 1;
  return count;
}
