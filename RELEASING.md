# Releasing

We version the project with [Semantic Versioning](https://semver.org/) and keep a
[changelog](CHANGELOG.md) in the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
format. Until the first stable release, versions stay below 1.0.0, starting at 0.1.0.

## Changelog

When your PR changes something users will notice, add a line under `## Unreleased` in
`CHANGELOG.md`, in the right section (`Added`, `Changed`, `Fixed`, `Removed`), with a link to
the PR. Internal changes like refactors, tests or CI don't need an entry.

## Making a release

1. Decide the version. While we're below 1.0.0, bump the minor version (0.1.0 → 0.2.0) for new
   features or breaking changes, and the patch version (0.1.0 → 0.1.1) for fixes only.
2. Open a PR titled `chore: release vX.Y.Z` that renames `## Unreleased` in `CHANGELOG.md` to
   `## X.Y.Z` and adds a new, empty `## Unreleased` above it. Merge it.
3. Create a GitHub release: on the Releases page, click **Draft a new release**, type the new
   tag `vX.Y.Z` (target `main`), and paste that version's changelog section as the description.
   Publishing the release builds the backend image (`ghcr.io/p-k-k-a/pkka-backend:vX.Y.Z`)
   and deploys the web app to Vercel. You can follow both in the Actions tab.
4. Deploy the backend on the server with `./deploy.sh vX.Y.Z` from `pkka-infra/deploy`. It
   backs up the database before switching to the new image.

The web app goes live in step 3, before the backend. If the new web app depends on a backend
change, deploy the backend right after publishing the release.

## Rolling back

For the web app, promote the previous production deployment in the Vercel dashboard. For the
backend, run `./deploy.sh` with the previous tag. A database migration is not undone by
rolling back the image; restore the backup made before the release instead (see
`pkka-infra/deploy/restore.sh`).

## Setup

The Vercel deploy needs three repository secrets: `VERCEL_TOKEN`, `VERCEL_ORG_ID` and
`VERCEL_PROJECT_ID`. You can find the two IDs in `.vercel/project.json` after running
`npx vercel link` in `frontend/apps/web`.
