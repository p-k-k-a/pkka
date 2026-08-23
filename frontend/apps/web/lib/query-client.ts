import { createQueryClient } from "@pkka/domain";
import { QueryClient } from "@tanstack/react-query";

let browserQueryClient: QueryClient | undefined;

export function getQueryClient() {
  // The server renders each request in isolation, so it always gets a fresh
  // client; the browser keeps one so the cache survives navigation.
  if (typeof window === "undefined") {
    return createQueryClient();
  }

  browserQueryClient ??= createQueryClient();
  return browserQueryClient;
}
