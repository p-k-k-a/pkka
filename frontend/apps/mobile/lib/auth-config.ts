export const API_URL = process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080";
export const KEYCLOAK_URL =
  process.env.EXPO_PUBLIC_KEYCLOAK_URL ??
  "http://localhost:8080/oauth2/authorization/keycloak-mobile";
export const ACCOUNT_CONSOLE_URL =
  process.env.EXPO_PUBLIC_KEYCLOAK_ACCOUNT_URL ?? "http://localhost:8081/realms/pkka/account";
export const DISCORD_URL = process.env.EXPO_PUBLIC_DISCORD_URL ?? "";
export const REDIRECT_URI = "pkka://";
