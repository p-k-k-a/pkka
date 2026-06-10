---
name: api-data-mocks
description: |
  Use this skill whenever a screen or component in this repo needs to read
  or display backend data — fetching a list, a detail record, or anything
  served by the API. Trigger on phrases like "fetch X", "load X from the
  API", "use the API", "make it use real data", "use the mocks", "MSW",
  "orval", "add an endpoint", "generated client", and any time you are about
  to hardcode a demo/placeholder data object in a screen. The hard rule this
  skill enforces: NEVER hardcode a `DEMO_*` data constant inside a screen.
  Data always flows through the orval-generated react-query hooks, backed by
  the hand-written MSW mock in `msw.setup.ts`. This skill captures the full
  spec → generate → mock → consume workflow so it is done right the first
  time.
---

# API data + MSW mocks workflow

This repo has a deliberate data layer. **Screens never hold hardcoded data.**
Every piece of backend data is fetched through a generated react-query hook
and mocked by MSW in development. If you find yourself writing a
`const DEMO_POST = {...}` inside a screen — stop, and follow this skill
instead.

## The pieces

| File | Role |
| --- | --- |
| `frontend/openapi.yaml` | **Source of truth.** Hand-edited OpenAPI spec — paths + `components.schemas`. |
| `frontend/packages/api/orval.config.ts` | Orval config. `client: react-query`, `mode: split`, `mock: true`. |
| `frontend/packages/api/src/generated/api.ts` | Generated react-query hooks (`useGetX`). **Do not hand-edit.** |
| `frontend/packages/api/src/generated/schemas/` | Generated TS types (`Announcement`, `BlogPost`, ...). **Do not hand-edit.** |
| `frontend/packages/api/src/generated/api.msw.ts` | Generated MSW handlers — gibberish faker data, **not used at runtime**. |
| `frontend/apps/mobile/msw.setup.ts` | **Hand-written runtime mock.** Monkey-patches `global.fetch`, returns nice faker data per endpoint. |
| `frontend/apps/mobile/app/_layout.tsx` | `require("../msw.setup")` under `__DEV__`; mounts `QueryClientProvider`. |

`@pkka/api` is the workspace package. Import hooks and types from it:
`import { useGetX, type SomeSchema } from "@pkka/api";`

## Why two mocks exist

Orval's generated `api.msw.ts` mocks every string field with
`faker.string.alpha` — unreadable gibberish. So the project ignores those
handlers at runtime and instead hand-writes readable faker data in
`msw.setup.ts` (real sentences, names, image URLs). The generated file is
kept only because `mock: true` is on; don't wire it into the app.

## Adding a new data-backed screen — the 4 steps

### 1. Define the endpoint + schema in `frontend/openapi.yaml`

Add a `path` with an `operationId` (drives the hook name —
`getBlogPostBySlug` → `useGetBlogPostBySlug`) and a `components.schemas`
entry. Mark required fields. `x-faker` hints are allowed for documentation
but **orval ignores them** — don't rely on them for mock quality.

```yaml
paths:
  /api/blog-posts/{slug}:
    get:
      summary: Pobiera pojedynczy wpis bloga
      operationId: getBlogPostBySlug
      parameters:
        - name: slug
          in: path
          required: true
          schema:
            type: string
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/BlogPost"
```

### 2. Regenerate the client

```bash
cd frontend/packages/api && pnpm generate
```

This rewrites `api.ts`, `schemas/`, and `api.msw.ts` from the spec. New hook,
new type, new schema export in `schemas/index.ts` all appear automatically.

### 3. Add a runtime mock in `frontend/apps/mobile/msw.setup.ts`

Write a `generateMockX()` returning the **generated type**, with readable
faker data, then add a branch to the `global.fetch` override. Match the URL
the generated `getGetXUrl()` produces.

```ts
import type { BlogPost } from "@pkka/api";

const generateMockBlogPost = (): BlogPost => ({
  id: faker.string.uuid(),
  title: faker.lorem.sentence(),
  content: faker.lorem.paragraphs(3, "\n\n"),
  publishedAt: faker.date.recent().toISOString(),
  imageUrl: faker.image.url(),     // startsWith("http") so <Image> renders
  // ...all required fields
});

// inside the global.fetch override:
if (url.includes("/api/blog-posts/")) {
  return new Response(JSON.stringify(generateMockBlogPost()), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}
```

### 4. Consume the hook in the screen — no hardcoded data

```tsx
import { useGetBlogPostBySlug, type BlogPost } from "@pkka/api";

const { data, isError } = useGetBlogPostBySlug(slug ?? "", {
  query: { enabled: !!slug },        // guard optional route params
});
const post = data?.data;             // apiFetch wraps payload in { data, status, headers }

if (isError) return <ErrorText />;
if (!post) return <ActivityIndicator />;
return <Content post={post} />;
```

`apiFetch` (the orval mutator at `frontend/packages/api/mutator.ts`) returns
`{ data, status, headers }`, so the payload is always `query.data?.data`.
Loading/error UI: mirror `app/(tabs)/index.tsx` — `ActivityIndicator` while
loading, a muted `Text` on error.

## Checklist before declaring done

- [ ] No `DEMO_*` / hardcoded data object left in any screen.
- [ ] `frontend/openapi.yaml` has the path + schema.
- [ ] `pnpm generate` ran; `api.ts` + `schemas/` reflect the new endpoint.
- [ ] `msw.setup.ts` has a `generateMockX()` and a `fetch` branch for the URL.
- [ ] Screen uses the generated `useGetX` hook with loading + error states.
- [ ] `npx tsc --noEmit` passes in `apps/mobile` and `packages/api`.
