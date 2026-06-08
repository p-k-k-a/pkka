import path from "node:path";
import { fileURLToPath } from "node:url";

import dotenv from "dotenv";
import { defineConfig } from "orval";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.resolve(__dirname, "../../.env") });

const frontendRoot = path.resolve(__dirname, "../..");
const repoRoot = path.resolve(frontendRoot, "..");
const fallbackInput = path.resolve(repoRoot, "backend/openapi.json");
const raw = process.env.OPENAPI_INPUT?.trim() || fallbackInput;
const input = /^https?:\/\//i.test(raw)
  ? raw
  : path.isAbsolute(raw)
    ? raw
    : path.resolve(repoRoot, raw);

export default defineConfig({
  pkka: {
    input,
    output: {
      mode: "split",
      target: "./src/generated/api.ts",
      schemas: "./src/generated/schemas",
      client: "react-query",
      mock: false,
      override: {
        mutator: { path: "./mutator.ts", name: "apiFetch" },
        query: { useQuery: true, useMutation: true, signal: true, usePrefetch: true },
      },
      formatter: "prettier",
    },
  },
});
