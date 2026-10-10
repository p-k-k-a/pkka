# Releasing

A release is a `vX.Y.Z` tag. Pushing it builds the backend image and deploys the web app to
Vercel (pkka.me). The backend is then deployed on the VM by hand. The mobile app is not part
of this flow yet.

## In every PR: add a changelog entry

If users will notice the change, add one line to [CHANGELOG.md](CHANGELOG.md) under
`## Unreleased`, in the matching section, with the PR link:

```md
### Added

- Waitlist for full events [#198](https://github.com/p-k-k-a/pkka/pull/198)
```

Sections, from [Keep a Changelog](https://keepachangelog.com/en/1.1.0/): `Added`, `Changed`,
`Deprecated`, `Removed`, `Fixed`, `Security`. Write for users, not for developers. Refactors,
CI and test-only changes need no entry.

## Cutting a release (about 15 minutes)

1. **Pick the version.** Follow [Semantic Versioning](https://semver.org/). Until the
   first stable release, versions stay at `0.y.z`, starting with `0.1.0`:

   | Unreleased contains                            | Before 1.0.0  | From 1.0.0    |
   | ---------------------------------------------- | ------------- | ------------- |
   | a change that breaks the API or the mobile app | 0.4.2 → 0.5.0 | 1.4.2 → 2.0.0 |
   | anything under `Added`                         | 0.4.2 → 0.5.0 | 1.4.2 → 1.5.0 |
   | only `Fixed`                                   | 0.4.2 → 0.4.3 | 1.4.2 → 1.4.3 |

2. **Open the release PR.** On a branch from `main`, edit `CHANGELOG.md`: rename
   `## Unreleased` to `## X.Y.Z`, and add a new, empty `## Unreleased` above it.

   Title the PR `chore: release vX.Y.Z` and merge it.

3. **Tag the merge commit:**

   ```bash
   git switch main && git pull
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

   The tag starts two workflows (watch them under **Actions**):
   - **Build & publish backend image:** `ghcr.io/p-k-k-a/pkka-backend:vX.Y.Z` and `:latest`;
   - **Deploy web to Vercel:** pkka.me goes live with this version.

4. **Write the GitHub release.** Go to **Releases → Draft a new release**, pick the tag, and
   paste that version's section from `CHANGELOG.md` as the description. Or from the terminal:

   ```bash
   gh release create vX.Y.Z --title vX.Y.Z --notes "<paste the section>"
   ```

5. **Deploy the backend on the VM.** This step is manual because it backs up the database
   first:

   ```bash
   cd pkka-infra/deploy
   ./deploy.sh vX.Y.Z
   ```

   If the release needs new environment variables, add them to `.env` first.

6. **Check production.** Open https://pkka.me, log in, and do one thing that writes data, for
   example sign up for an event.

The web app goes live in step 3, before the backend in step 5. If a release changes the API in
a way the old backend cannot serve, run step 5 straight after step 3, or ship the backend
change in an earlier release.

## Rolling back

- **Web:** in the Vercel dashboard, open the project's **Deployments**, pick the previous
  production deployment and promote it. Or run **Actions → Deploy web to Vercel** with the
  previous tag selected.
- **Backend:** run `./deploy.sh <previous tag>` on the VM. If the bad release ran a Flyway
  migration, rolling the image back does not undo it. Restore the database from the dump that
  `deploy.sh` made before the release (`pkka-infra/deploy/restore.sh`).

## Redeploying a version

Run **Build & publish backend image** or **Deploy web to Vercel** from the Actions tab and
select the tag as the ref.

## One-time setup

In **Settings → Secrets and variables → Actions**, add `VERCEL_TOKEN`, `VERCEL_ORG_ID` and
`VERCEL_PROJECT_ID`. The two IDs are in `.vercel/project.json` after running
`npx vercel link` in `frontend/apps/web`.
