// Roles as returned by /api/me: Spring format, ROLE_ prefix stripped.

export const VERIFIED_ALUMN_ROLE = "VERIFIED_ALUMN";

export function hasRole(roles: string[] | undefined | null, role: string) {
  return roles?.includes(role) ?? false;
}

export function isVerifiedAlumn(roles: string[] | undefined | null) {
  return hasRole(roles, VERIFIED_ALUMN_ROLE);
}
