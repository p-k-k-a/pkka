# pkka

Spring Boot backend + TypeScript frontend (Next.js web, Expo mobile) sharing a typed API client generated from the backend's OpenAPI spec.

## Approximate layout

```
pkka/
├── backend/                Spring Boot service
└── frontend/               pnpm + Turborepo workspace
    ├── apps/
    │   ├── web/            Next.js
    │   └── mobile/         Expo
    └── packages/
        └── api/            Generated API client (web + mobile)
```

## Setup

### 1. Run the backend first

IntelliJ is recommended for development

It must be reachable so the frontend can read its OpenAPI spec at `http://localhost:8080/v3/api-docs`.

### 2. Configure the frontend env

```bash
cd frontend
cp .env.example .env
```

Set `OPENAPI_INPUT` in `.env` to the spec URL:

```env
OPENAPI_INPUT=http://localhost:8080/v3/api-docs
```

### 3. Install and generate the API client

```bash
pnpm install
pnpm --filter @pkka/api generate
```

The generated files are not committed — rerun `generate` whenever the backend's API changes.

### 4. Run an app

```bash
pnpm --filter web dev       # http://localhost:3000
pnpm --filter mobile dev    # press `a` for Android, `i` for iOS
```
