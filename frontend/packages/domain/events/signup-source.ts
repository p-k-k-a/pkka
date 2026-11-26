import { RegistrationSource, type SignUpSourceResponse } from "@pkka/api";

export function signUpSourceFromRef(ref: string | null | undefined) {
  if (ref === "discord") return RegistrationSource.DISCORD;
  if (ref === "email") return RegistrationSource.EMAIL;
  return undefined;
}

export function sumSignUpSources(perEvent: SignUpSourceResponse[][]): SignUpSourceResponse[] {
  return Object.values(RegistrationSource).map((source) => ({
    source,
    signUps: perEvent.reduce(
      (sum, event) => sum + (event.find((row) => row.source === source)?.signUps ?? 0),
      0,
    ),
  }));
}
