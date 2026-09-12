import { createQueryClient } from "@pkka/api";
import { QueryClient } from "@tanstack/react-query";

let browserQueryClient: QueryClient | undefined;

export function getQueryClient() {
  if (typeof window === "undefined") {
    return createQueryClient();
  }

  browserQueryClient ??= createQueryClient();
  return browserQueryClient;
}
