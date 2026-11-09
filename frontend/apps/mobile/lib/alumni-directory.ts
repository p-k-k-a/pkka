import { useDebouncedValue } from "@/lib/use-debounced-value";
import { useListAlumniInfinite, type AlumniListItemResponse } from "@pkka/api";
import { buildAlumniParams, type AlumniFilters, type AlumniSortOption } from "@pkka/domain";
import { useMemo } from "react";

type UseAlumniDirectoryArgs = {
  query: string;
  filters: AlumniFilters;
  sort: AlumniSortOption;
};

export function useAlumniDirectory({ query, filters, sort }: UseAlumniDirectoryArgs) {
  const debouncedQuery = useDebouncedValue(query);
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
