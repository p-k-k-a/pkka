import type { RegistrationSource } from "@pkka/api";
import { signUpSourceFromRef } from "@pkka/domain";

const sources = new Map<string, RegistrationSource>();

export function rememberSignUpSource(slug: string, ref: string): void {
  const source = signUpSourceFromRef(ref);
  if (source) sources.set(slug, source);
}

export function savedSignUpSource(slug: string): RegistrationSource | null {
  return sources.get(slug) ?? null;
}

export function forgetSignUpSource(slug: string): void {
  sources.delete(slug);
}
