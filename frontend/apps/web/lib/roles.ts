export const ADMIN_ROLE = "admin";

function normalizeRole(role: string): string {
  return role.toLowerCase().replace(/_/g, "-");
}

export function hasRole(roles: string[] | undefined | null, role: string): boolean {
  return (roles ?? []).map(normalizeRole).includes(normalizeRole(role));
}

export function isAdmin(roles: string[] | undefined | null): boolean {
  return hasRole(roles, ADMIN_ROLE);
}
