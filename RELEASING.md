# Releasing

A release publishes the backend image to GHCR and the web app to Vercel (pkka.me), both
at the same version. Release notes and [CHANGELOG.md](CHANGELOG.md) are written for you
from the titles of merged PRs.

The mobile app is not part of this flow yet.

## How versions and notes are made

PRs are squash-merged, so each PR title becomes one commit on `main`. CI already checks
that titles follow [Conventional Commits](https://www.conventionalcommits.org/). The type
decides the next version and where the change shows up in the notes:

| PR title                                                    | Next version         | Changelog section    |
| ----------------------------------------------------------- | -------------------- | -------------------- |
| `fix(web): …`                                               | patch: 1.2.0 → 1.2.1 | Bug Fixes            |
| `feat(backend): …`                                          | minor: 1.2.0 → 1.3.0 | Features             |
| `feat!: …` (any type with `!`)                              | major: 1.2.0 → 2.0.0 | Breaking changes     |
| `perf: …`, `revert: …`                                      | patch                | Performance, Reverts |
| `refactor`, `docs`, `test`, `ci`, `build`, `chore`, `style` | none on their own    | hidden               |

Write the PR title for the people who will read the release notes: it is copied into the
changelog word for word.

## Release step by step

1. **Merge PRs into `main` as usual.** After each merge, the
   [Release workflow](.github/workflows/release.yml) runs
   [release-please](https://github.com/googleapis/release-please). It opens, or updates, a PR
   titled `chore(main): release X.Y.Z`. That PR bumps `version.txt` and
   `.release-please-manifest.json` and adds the new section to `CHANGELOG.md`.
2. **Review the release PR.** Check that the version and the notes are what you expect.
   To change an entry, see [Fixing the notes](#fixing-the-notes).
3. **Merge the release PR when you want to ship.** Follow the run under **Actions →
   Release**. It:
   - creates the tag `vX.Y.Z` and a GitHub release with the same notes;
   - builds `ghcr.io/p-k-k-a/pkka-backend:vX.Y.Z` (and `:latest`);
   - deploys the web app to Vercel production.
4. **Deploy the backend on the VM.** This step stays manual on purpose, because it takes a
   database backup first:

   ```bash
   cd pkka-infra/deploy
   ./deploy.sh vX.Y.Z
   ```

   If the release needs new environment variables, add them to `.env` first.

5. **Check production.** Open https://pkka.me, log in, and try one action that writes data,
   for example signing up for an event.

The web app goes live in step 3, before the backend in step 4. If a release changes the API
in a way the old backend cannot serve, run step 4 straight after the merge, or ship the
backend change in an earlier release.

## Rolling back

- **Web:** in the Vercel dashboard, open the project's **Deployments**, pick the previous
  production deployment and promote it (Instant Rollback). Or run **Actions → Deploy web to
  Vercel** on the previous tag.
- **Backend:** `./deploy.sh <previous tag>` on the VM. If the bad release ran a Flyway
  migration, rolling the image back does not undo it; restore the database from the dump
  `deploy.sh` made before the release (see `pkka-infra/deploy/restore.sh`).

## Fixing the notes

- **Before the release PR is merged:** open the original, already merged PR and add this
  to its description:

  ```
  BEGIN_COMMIT_OVERRIDE
  fix(web): the corrected title
  END_COMMIT_OVERRIDE
  ```

  On its next run, release-please uses that text instead of the squash commit message and
  regenerates the release PR. Edits made directly in the release PR are not reliable: it is
  regenerated after every merge to `main`.

- **After the release:** edit the GitHub release text, and fix `CHANGELOG.md` in a normal PR.
- **Forcing a version:** merge a commit whose body contains `Release-As: 2.0.0`, for
  example `git commit --allow-empty -m "chore: release 2.0.0" -m "Release-As: 2.0.0"`.

## Releasing without the release PR

To redeploy an existing version, run the workflow by hand from the Actions tab:
**Build & publish backend image** or **Deploy web to Vercel**, with the tag selected as the
ref. Pushing a `v*` tag by hand also starts both workflows, but it skips the changelog, so
prefer the release PR.

## One-time setup

- **Settings → Actions → General → Workflow permissions:** enable _Allow GitHub Actions to
  create and approve pull requests_, so release-please can open its PR.
- **Settings → Secrets and variables → Actions:** add `VERCEL_TOKEN`, `VERCEL_ORG_ID` and
  `VERCEL_PROJECT_ID`. The two IDs are in `.vercel/project.json` after running
  `npx vercel link` in `frontend/apps/web`.

The release PR is opened with GitHub's built-in token, so CI does not run on it on its own.
It only changes `version.txt`, the manifest and the changelog, so that is safe to merge
without checks.
