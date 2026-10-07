// Mirrors the @Pattern constraints on UpdateProfileRequest. A pasted "copy link" carries
// tracking params the backend rejects, so canonicalize before validating or submitting.
const LINKEDIN_PROFILE = /^https:\/\/([a-z]{2,3}\.)?linkedin\.com\/in\/[^/?#]+$/i;
const GITHUB_PROFILE =
  /^https:\/\/(www\.)?github\.com\/[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$/i;

/**
 * Reduces a pasted profile link to the bare canonical form the backend stores:
 * adds a missing scheme, forces https, drops any `?query` / `#fragment`, and trims
 * trailing slashes. Blank input stays blank — that's how a field gets cleared.
 *
 *   "linkedin.com/in/jan"                     -> "https://linkedin.com/in/jan"
 *   "https://github.com/jan?tab=repositories" -> "https://github.com/jan"
 *   "http://pl.linkedin.com/in/jan/"          -> "https://pl.linkedin.com/in/jan"
 */
export function canonicalizeProfileUrl(raw: string): string {
  const trimmed = raw.trim();
  if (!trimmed) return "";
  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  return withScheme
    .replace(/^http:\/\//i, "https://")
    .replace(/[?#].*$/, "")
    .replace(/\/+$/, "");
}

export function linkedinUrlError(raw: string): string | undefined {
  const url = canonicalizeProfileUrl(raw);
  if (!url || LINKEDIN_PROFILE.test(url)) return undefined;
  return "Podaj link do profilu, np. https://www.linkedin.com/in/jan-kowalski";
}

export function githubUrlError(raw: string): string | undefined {
  const url = canonicalizeProfileUrl(raw);
  if (!url || GITHUB_PROFILE.test(url)) return undefined;
  return "Podaj link do profilu, np. https://github.com/jankowalski";
}
