import { useDebouncedValue } from "@/lib/use-debounced-value";
import { useListAlumniInfinite, type AlumniListItemResponse } from "@pkka/api";
import {
  buildAlumniParams,
  type AlumniFilters,
  type AlumniSortOption as SortOption,
} from "@pkka/domain";
import { useMemo } from "react";

export {
  ALUMNI_YEAR_MAX as YEAR_MAX,
  ALUMNI_YEAR_MIN as YEAR_MIN,
  ALUMNI_SORT_OPTIONS as SORT_OPTIONS,
  countActiveAlumniFilters as countActiveFilters,
  DEFAULT_ALUMNI_SORT as DEFAULT_SORT,
  EMPTY_ALUMNI_FILTERS as EMPTY_FILTERS,
  type AlumniFilters,
  type AlumniSortOption as SortOption,
} from "@pkka/domain";

const SEARCH_DEBOUNCE_MS = 300;

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
