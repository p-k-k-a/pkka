import type { RegistrationSource } from "@pkka/api";
import { signUpSourceFromRef } from "@pkka/domain";

const storageKey = (slug: string) => `signup_source:${slug}`;

export function captureSignUpSource(slug: string) {
  const url = new URL(window.location.href);
  if (!url.searchParams.has("ref")) return;
  const source = signUpSourceFromRef(url.searchParams.get("ref"));
  try {
    if (source) sessionStorage.setItem(storageKey(slug), source);
  } catch {}
  url.searchParams.delete("ref");
  window.history.replaceState(null, "", url);
}

export function savedSignUpSource(slug: string) {
  try {
    return (sessionStorage.getItem(storageKey(slug)) as RegistrationSource | null) ?? undefined;
  } catch {
    return undefined;
  }
}

export function forgetSignUpSource(slug: string) {
  try {
    sessionStorage.removeItem(storageKey(slug));
  } catch {}
}
