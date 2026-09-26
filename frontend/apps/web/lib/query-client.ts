import { createQueryClient } from "@pkka/api";
import { QueryClient } from "@tanstack/react-query";

const STALE_TIME_MS = 60_000;

let browserQueryClient: QueryClient | undefined;

export function getQueryClient() {
  if (typeof window === "undefined") {
    return createQueryClient({ staleTime: STALE_TIME_MS });
  }

  browserQueryClient ??= createQueryClient({ staleTime: STALE_TIME_MS });
  return browserQueryClient;
}
