export const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function keycloakLoginUrl() {
  return new URL("/oauth2/authorization/keycloak-web", apiBaseUrl).toString();
}

export function backendLogoutUrl() {
  return new URL("/logout", apiBaseUrl).toString();
}
