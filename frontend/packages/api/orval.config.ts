import path from "node:path";
import { fileURLToPath } from "node:url";

import dotenv from "dotenv";
import { defineConfig } from "orval";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.resolve(__dirname, "../../.env") });

const raw = process.env.OPENAPI_INPUT;
if (!raw) {
  throw new Error(
    "OPENAPI_INPUT is not set. Copy frontend/.env.example to frontend/.env and set OPENAPI_INPUT to the OpenAPI spec URL or path.",
  );
}

const monorepoRoot = path.resolve(__dirname, "../..");
const input = /^https?:\/\//i.test(raw) ? raw : path.resolve(monorepoRoot, raw);

export default defineConfig({
  pkka: {
    input,
    output: {
      mode: "split",
      target: "./src/generated/api.ts",
      schemas: "./src/generated/schemas",
      client: "react-query",
<<<<<<< HEAD
      mock: true, //tu dodoałem coś zeby były mocki
=======
      mock: true,
>>>>>>> 8b0b4800 (refactor(web): split provider setup and improve dev msw/api integration)
      override: {
        mutator: { path: "./mutator.ts", name: "apiFetch" },
        query: { useQuery: true, useMutation: true, signal: true, usePrefetch: true },
      },
      formatter: "prettier",
    },
  },
});
