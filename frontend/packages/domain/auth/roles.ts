// Roles as returned by /api/me: Spring format, ROLE_ prefix stripped.

export const ADMIN_ROLE = "ADMIN";
export const VERIFIED_ALUMN_ROLE = "VERIFIED_ALUMN";

export function hasRole(roles: string[] | undefined | null, role: string): boolean {
  return roles?.includes(role) ?? false;
}

export function isAdmin(roles: string[] | undefined | null): boolean {
  return hasRole(roles, ADMIN_ROLE);
}

export function isVerifiedAlumn(roles: string[] | undefined | null): boolean {
  return hasRole(roles, VERIFIED_ALUMN_ROLE);
}
