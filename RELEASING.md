# Releasing

We version the project with [Semantic Versioning](https://semver.org/) and keep a
[changelog](CHANGELOG.md) in the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
format.

## Changelog

When your PR changes something users will notice, add a line under `## Unreleased` in
`CHANGELOG.md`, in the right section (`Added`, `Changed`, `Fixed`, `Removed`), with a link to
the PR. Internal changes like refactors, tests or CI don't need an entry.

## Making a release

1. Decide the version: bump **patch** for fixes (`v1.1.0` → `v1.1.1`), **minor** for features
   or breaking changes (`v1.1.0` → `v1.2.0`).
2. Open a PR titled `chore: release vX.Y.Z` that:
   - renames `## Unreleased` in `CHANGELOG.md` to `## X.Y.Z` and adds a new, empty
     `## Unreleased` above it;
   - sets `"version"` in `frontend/apps/mobile/app.json` to `X.Y.Z`, the version users see in
     the Android app. (EAS bumps the internal build number on its own.)

   Merge it.

3. Create a GitHub release: on the Releases page, click **Draft a new release**, type the new
   tag `vX.Y.Z` (target `main`), and paste that version's changelog section as the description.
   **Publishing the release** (workflow `Build & publish backend image`, triggered by
   `release: published`) runs in parallel:
   - builds and pushes `ghcr.io/p-k-k-a/pkka-backend:vX.Y.Z` (and `:latest`);
   - deploys the web app to Vercel (`Deploy web to Vercel`);
   - starts the Android build on EAS (`Build Android app`);
   - rolls the backend out on the production VM via `az vm run-command` and
     `pkka-infra/deploy/deploy.sh` (job `Deploy backend to the VM`, environment `production`).

   Follow progress in the Actions tab. The Android build runs on Expo's servers and can take a
   while on the free plan; the APK shows up on expo.dev when it's done.

The web app goes live in step 3, before the backend container finishes its health check. If the
new web app depends on a backend change, publish the release only when you are ready for that
deploy (or redeploy the backend right after — see below).

## Backend deploy (CI and manual)

Production deploys use `deploy.sh` on the VM (`/opt/pkka/deploy/deploy.sh` in
[pkka-infra](https://github.com/p-k-k-a/pkka-infra)). The script validates the tag, takes a
lock, **backs up both databases**, pulls the image, updates `BACKEND_TAG` in `.env`, runs
`docker compose up --wait` on the backend, and prints a final line
`PKKA_DEPLOY_RESULT=OK|ROLLED_BACK|FAILED` for CI to parse.

### What triggers what

| Trigger | Build image? | Deploy to VM? | Tag used |
| --- | --- | --- | --- |
| **Release published** | yes | yes | `release.tag_name` (`vX.Y.Z`) |
| **Run workflow**, input **tag filled** (e.g. `v1.1.0`) | no | yes (redeploy existing image) | input tag |
| **Run workflow**, input **tag empty** | yes (from selected ref) | no | — |

In GitHub: **Actions → Build & publish backend image → Run workflow**. Choose the branch only
when building without deploy; for redeploy, pick any ref (e.g. `main`) and set **tag** to an
image that already exists in GHCR.

Redeploying the **same** tag does not necessarily recreate the container: if the pulled image
digest is unchanged, Compose keeps the running container (uptime in `docker ps` stays the same).
The script still runs backup, pull, and the health check.

Tags must match `vX.Y.Z` (optional `-rc.1` suffix). Invalid tags fail the deploy job.

### Manual deploy on the server (fallback)

If CI is unavailable:

```bash
ssh pkka-deploy@<VM>
cd /opt/pkka/deploy
./deploy.sh vX.Y.Z
```

Use the version from [pkka-infra](https://github.com/p-k-k-a/pkka-infra) `deploy/deploy.sh`
(LF line endings; `chmod +x` after copy from Windows).

## Rolling back

- **Web:** promote the previous production deployment in the Vercel dashboard.
- **Backend:** redeploy the previous tag — GitHub Actions with **Run workflow** and input
  `tag=vX.Y.Z` (previous good version), or `./deploy.sh vX.Y.Z` on the VM.

Rolling back the image does **not** undo Flyway migrations; restore the database backup taken
before the bad release (`pkka-infra/deploy/restore.sh`).

## Setup (one-time)

### Vercel (web)

Repository secrets: `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`. Find the two IDs in
`.vercel/project.json` after `npx vercel link` in `frontend/apps/web`.

### Expo (Android)

The `pkka` Expo project id is in `frontend/apps/mobile/app.json`. Once:

1. `npx eas-cli credentials -p android` in `frontend/apps/mobile` — EAS keeps the signing key;
   every future APK must use it.
2. `EXPO_TOKEN` repository secret ([expo.dev access tokens](https://expo.dev/settings/access-tokens)
   or a robot user for the organization).

The free Expo plan includes 15 Android builds a month.

### Azure (backend VM deploy)

GitHub authenticates with **OIDC** (no Azure password in secrets). A user-assigned managed
identity on the VM resource group has a federated credential with subject
`repo:p-k-k-a/pkka:environment:production` and a custom role allowing
`Microsoft.Compute/virtualMachines/runCommand/action` on the production VM.

Repository **secrets:** `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`.

Repository **variables:** `AZURE_RESOURCE_GROUP`, `AZURE_VM_NAME`.

Optional: add required reviewers on the GitHub **production** environment to gate deploys.

The VM must have an up-to-date `deploy.sh` (from pkka-infra) and a valid `.env` including
`BACKEND_TAG`, database passwords, Keycloak secrets, and `WEB_*` URLs for the live frontend.
