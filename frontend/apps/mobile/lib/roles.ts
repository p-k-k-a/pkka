// Keycloak realm roles, mirrored from the web app's lib/roles.ts. Roles are
// normalized (lowercased, `_` → `-`) before comparison so `verified_alumn`,
// `VERIFIED-ALUMN`, etc. all match the same role.

export const VERIFIED_ALUMN_ROLE = "verified-alumn";

const normalize = (role: string) => role.toLowerCase().replace(/_/g, "-");

export function hasRole(roles: string[] | undefined | null, role: string) {
  if (!roles) return false;
  const target = normalize(role);
  return roles.some((r) => normalize(r) === target);
}

export function isVerifiedAlumn(roles: string[] | undefined | null) {
  return hasRole(roles, VERIFIED_ALUMN_ROLE);
}
