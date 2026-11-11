export const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const WEB_REGISTRATION_ID = "keycloak-web";

function authorizationUrl(params?: Record<string, string>) {
  const url = new URL(`/oauth2/authorization/${WEB_REGISTRATION_ID}`, apiBaseUrl);
  for (const [key, value] of Object.entries(params ?? {})) {
    url.searchParams.set(key, value);
  }
  return url.toString();
}

export function keycloakLoginUrl() {
  return authorizationUrl();
}

export function discordLoginUrl() {
  return authorizationUrl({ idp: "discord" });
}

export function registerUrl() {
  return authorizationUrl();
}

export function backendLogoutUrl() {
  return new URL("/logout", apiBaseUrl).toString();
}
