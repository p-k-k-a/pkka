export const ADMIN_ROLE = "admin";
export const VERIFIED_ALUMN_ROLE = "verified-alumn";

function normalizeRole(role: string): string {
  return role.toLowerCase().replace(/_/g, "-");
}

export function hasRole(roles: string[] | undefined | null, role: string): boolean {
  return (roles ?? []).map(normalizeRole).includes(normalizeRole(role));
}

export function isAdmin(roles: string[] | undefined | null): boolean {
  return hasRole(roles, ADMIN_ROLE);
}

export function isVerifiedAlumn(roles: string[] | undefined | null): boolean {
  return hasRole(roles, VERIFIED_ALUMN_ROLE);
}
