import { defineConfig } from "orval";

export default defineConfig({
  pkka: {
    input: "../../../backend/openapi.json",
    output: {
      mode: "split",
      target: "./src/generated/api.ts",
      schemas: "./src/generated/schemas",
      client: "react-query",
      override: {
        mutator: { path: "./mutator.ts", name: "apiFetch" },
        query: { useQuery: true, useMutation: true, signal: true, usePrefetch: true },
      },
      formatter: "prettier",
    },
  },
});
