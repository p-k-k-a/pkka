import { RegistrationSource } from "@pkka/api";

export function signUpSourceFromRef(ref: string | null | undefined) {
  if (ref === "discord") return RegistrationSource.DISCORD;
  if (ref === "email") return RegistrationSource.EMAIL;
  return undefined;
}
