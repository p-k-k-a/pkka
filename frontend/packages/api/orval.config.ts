import path from "node:path";
import { fileURLToPath } from "node:url";

import dotenv from "dotenv";
import { defineConfig } from "orval";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.resolve(__dirname, "../../.env") });

const input = process.env.OPENAPI_INPUT;

export default defineConfig({
  pkka: {
    input,
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
