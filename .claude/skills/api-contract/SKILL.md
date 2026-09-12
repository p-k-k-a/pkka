---
name: api-contract
description: |
  Use this skill whenever changing the shape of the HTTP contract — adding or
  editing a Java enum, DTO, record or controller signature under `backend/`, or
  regenerating `@pkka/api`. Trigger on "add an enum", "new DTO", "new endpoint",
  "regenerate the client", "openapi", "orval", and on edits to
  `backend/src/main/java/**/*.java` or `backend/openapi.json`. Covers the
  `enumAsRef` rule that keeps one enum from becoming several TypeScript types,
  and the regeneration workflow with its known hazards.
---

# Backend ↔ generated client contract

The frontend never hand-writes API types. `backend/openapi.json` is dumped from a
running server, and `@pkka/api` is generated from it by orval. Anything sloppy in the
Java annotations becomes sloppy TypeScript in both apps.

## Every enum in the API surface needs `@Schema(enumAsRef = true)`

**This is the rule.** Without it springdoc inlines the enum into every DTO that uses
it, as an anonymous list of values:

```json
"CreateApplicationRequest": { "properties": {
  "faculty": { "type": "string", "enum": ["WE", "WEGH", …] } } },
"ApplicationResponse":      { "properties": {
  "faculty": { "type": "string", "enum": ["WE", "WEGH", …] } } }
```

Orval cannot tell those two anonymous enums are one concept, so it emits one
TypeScript object **per schema that mentions it**, named after that schema —
`CreateApplicationRequestFaculty`, `ApplicationResponseFaculty`. They have identical
members and are mutually incompatible types. Call sites then need casts, and shared
code has to be made generic for no reason.

With the annotation on the enum class:

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Faculty { … }
```

springdoc emits one component and both DTOs point at it:

```json
"Faculty": { "type": "string", "enum": ["WE", "WEGH", …] },
"CreateApplicationRequest": { "properties": {
  "faculty": { "$ref": "#/components/schemas/Faculty", "description": "AGH faculty code" } } }
```

and orval generates a single `Faculty`, usable on both sides.

Applying this to the nine existing enums turned 26 inline enum definitions into 9 shared
components, and removed 74 call-site references to duplicated names.

Notes:

- The annotation goes on the **enum class**, not on the field that uses it.
- There is no global switch for this — every new enum needs the annotation. Adding an
  enum without it silently reintroduces the duplication.
- A field-level `@Schema(description = …, example = …)` next to the enum is fine. The
  spec is OpenAPI 3.1, where `$ref` may carry sibling keywords.
- A **custom** `@Schema` on the enum class itself (beyond `enumAsRef`) can make
  springdoc fall back to inlining. Keep enum classes annotation-light.
- It applies to query-parameter enums too, not just DTO fields.

## Regenerating the client

Generated files are gitignored, so a fresh checkout has no `packages/api/src/generated`
until this runs.

1. Start the backend — `OpenApiDumper` rewrites `backend/openapi.json` on startup, in
   the working directory the server was started from.
2. `rm -rf frontend/packages/api/src/generated` — **orval does not prune its output.**
   Renamed or removed schemas leave stale files behind, and because `index.ts` is
   rewritten, typecheck still passes and hides them.
3. `pnpm --filter @pkka/api generate`
4. `pnpm turbo run lint typecheck build` from `frontend/`.

## Hazards with `backend/openapi.json`

- **springdoc emits properties in non-deterministic order.** The `Page*`, `Pageable`
  and `Sort` wrapper schemas churn on every restart even when nothing changed. Before
  assuming a diff is real, compare parsed JSON rather than text.
- **`./gradlew test` overwrites the file.** The dev-profile dumper is active under
  `@SpringBootTest`, and with no real port bound it falls back to 8080 and scrapes
  whatever server is listening there — possibly a colleague's branch. Tell-tale: schema
  names in the file disagree with the Java classes on your branch. Recover with
  `git checkout backend/openapi.json` and regenerate from a real server start.
- The file is committed **minified to one line**, so any change makes the whole file a
  textual conflict. Resolve semantically, never by hand-editing.

## Where the generated names surface

`packages/domain` imports `@pkka/api` with `import type` only — it must never gain a
runtime dependency on it. Anything needing an enum **value** at runtime (`Object.values`,
`instanceof ApiError`) belongs in the app or in `@pkka/api` itself, not in domain.
