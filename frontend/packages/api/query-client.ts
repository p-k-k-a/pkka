import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "./mutator";

export function createQueryClient({ staleTime }: { staleTime?: number } = {}): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime,
        // Don't retry client errors (4xx) — a 404 like "no application yet" is a
        // final answer, not a transient failure. Still retry network/5xx blips.
        retry: (failureCount, error) => {
          if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
            return false;
          }
          return failureCount < 3;
        },
      },
    },
  });
}
