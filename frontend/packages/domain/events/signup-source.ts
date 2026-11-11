import { RegistrationSource } from "@pkka/api";

export const SIGN_UP_REF_PARAM = "ref";

const LINK_SOURCES: Record<string, RegistrationSource> = {
  discord: RegistrationSource.DISCORD,
  email: RegistrationSource.EMAIL,
};

export function signUpSourceFromRef(ref: unknown): RegistrationSource | null {
  if (typeof ref !== "string") return null;
  return LINK_SOURCES[ref.trim().toLowerCase()] ?? null;
}
