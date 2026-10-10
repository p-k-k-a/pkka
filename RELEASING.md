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
2. Open a PR titled `chore: release vX.Y.Z` that:
   - renames `## Unreleased` in `CHANGELOG.md` to `## X.Y.Z` and adds a new, empty
     `## Unreleased` above it;
   - sets `"version"` in `frontend/apps/mobile/app.json` to `X.Y.Z`, the version users see in
     the Android app. (EAS bumps the internal build number on its own.)

   Merge it.

3. Create a GitHub release: on the Releases page, click **Draft a new release**, type the new
   tag `vX.Y.Z` (target `main`), and paste that version's changelog section as the description.
   Publishing the release builds the backend image (`ghcr.io/p-k-k-a/pkka-backend:vX.Y.Z`),
   deploys the web app to Vercel and starts the Android build on EAS. You can follow them in the
   Actions tab. The Android build itself runs on Expo's servers and can take a while on the
   free plan; the APK shows up on expo.dev when it's done, and you can attach it to the release.
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

The Android build uses the `pkka` Expo project (its id is in `frontend/apps/mobile/app.json`).
It needs, once:

1. `npx eas-cli credentials -p android` in `frontend/apps/mobile`, to let EAS generate and keep
   the signing key. Every future APK must be signed with it, or users can't update the app.
2. An Expo access token, added as the `EXPO_TOKEN` repository secret. Create it at
   https://expo.dev/settings/access-tokens, or as a robot user of the `pkka` organization so it
   isn't tied to one person.

The free Expo plan includes 15 Android builds a month.
