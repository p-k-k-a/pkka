export const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const WEB_REGISTRATION_ID = "keycloak-web";

function authorizationUrl(params: Record<string, string | undefined> = {}) {
  const url = new URL(`/oauth2/authorization/${WEB_REGISTRATION_ID}`, apiBaseUrl);
  for (const [key, value] of Object.entries(params)) {
    if (value) url.searchParams.set(key, value);
  }
  return url.toString();
}

export function keycloakLoginUrl(returnTo?: string) {
  return authorizationUrl({ returnTo });
}

export function discordLoginUrl(returnTo?: string) {
  return authorizationUrl({ idp: "discord", returnTo });
}

export function registerUrl(returnTo?: string) {
  return authorizationUrl({ returnTo });
}

export function backendLogoutUrl() {
  return new URL("/logout", apiBaseUrl).toString();
}
