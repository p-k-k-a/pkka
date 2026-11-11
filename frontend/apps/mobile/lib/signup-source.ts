import type { RegistrationSource } from "@pkka/api";
import { signUpSourceFromRef } from "@pkka/domain";

const sources = new Map<string, RegistrationSource>();

export function rememberSignUpSource(slug: string, ref: string) {
  const source = signUpSourceFromRef(ref);
  if (source) sources.set(slug, source);
}

export const savedSignUpSource = (slug: string) => sources.get(slug);

export const forgetSignUpSource = (slug: string) => sources.delete(slug);
