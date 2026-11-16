import { nextPageParam, pageItems } from "@/lib/pagination";
import { useDebouncedValue } from "@/lib/use-debounced-value";
import { useListMaterialsInfinite, type MaterialType } from "@pkka/api";
import { MATERIALS_PAGE_SIZE } from "@pkka/domain";
import { keepPreviousData } from "@tanstack/react-query";
import { useMemo } from "react";

type UseMaterialsArgs = {
  query: string;
  type: MaterialType | null;
  eventId: string | null;
};

export function useMaterials({ query, type, eventId }: UseMaterialsArgs) {
  const q = useDebouncedValue(query.trim());

  const result = useListMaterialsInfinite(
    {
      size: MATERIALS_PAGE_SIZE,
      ...(q ? { q } : {}),
      ...(type ? { type } : {}),
      ...(eventId ? { eventId } : {}),
    },
    {
      query: {
        initialPageParam: 0,
        getNextPageParam: nextPageParam,
        placeholderData: keepPreviousData,
      },
    },
  );

  const materials = useMemo(() => pageItems(result.data?.pages), [result.data]);

  return {
    materials,
    total: result.data?.pages[0]?.data.totalElements ?? materials.length,
    isLoading: result.isPending,
    isError: result.isError,
    refetch: result.refetch,
    fetchNextPage: result.fetchNextPage,
    hasNextPage: result.hasNextPage,
    isFetchingNextPage: result.isFetchingNextPage,
  };
}
