// Roles as returned by /api/me: Spring format, ROLE_ prefix stripped.

const ADMIN_ROLE = "ADMIN";
const VERIFIED_ALUMN_ROLE = "VERIFIED_ALUMN";

function hasRole(roles: string[] | undefined | null, role: string): boolean {
  return roles?.includes(role) ?? false;
}

export function isAdmin(roles: string[] | undefined | null): boolean {
  return hasRole(roles, ADMIN_ROLE);
}

export function isVerifiedAlumn(roles: string[] | undefined | null): boolean {
  return hasRole(roles, VERIFIED_ALUMN_ROLE);
}
