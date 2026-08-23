import { ApiError } from "@pkka/api";
import { QueryClient } from "@tanstack/react-query";

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,
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
