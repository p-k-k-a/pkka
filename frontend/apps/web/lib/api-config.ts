export const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function keycloakLoginUrl(idp?: "discord") {
  const url = new URL("/oauth2/authorization/keycloak-web", apiBaseUrl);
  if (idp === "discord") {
    url.searchParams.set("idp", "discord");
  }
  return url.toString();
}

export function backendLogoutUrl() {
  return new URL("/logout", apiBaseUrl).toString();
}
