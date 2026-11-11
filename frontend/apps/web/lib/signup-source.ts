import type { RegistrationSource } from "@pkka/api";
import { signUpSourceFromRef } from "@pkka/domain";

const storageKey = (slug: string) => `signup_source:${slug}`;

export function rememberSignUpSource(slug: string, ref: string): void {
  try {
    sessionStorage.setItem(storageKey(slug), ref);
  } catch {}
}

export function savedSignUpSource(slug: string): RegistrationSource | null {
  try {
    return signUpSourceFromRef(sessionStorage.getItem(storageKey(slug)));
  } catch {
    return null;
  }
}

export function forgetSignUpSource(slug: string): void {
  try {
    sessionStorage.removeItem(storageKey(slug));
  } catch {}
}
