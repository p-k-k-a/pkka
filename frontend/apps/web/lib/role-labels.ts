const ROLE_LABELS: Record<string, string> = {
  user: "Użytkownik",
  "verified-alumn": "Zweryfikowany absolwent",
  admin: "Administrator",
  bot: "Bot",
};

function normalizeRole(role: string): string {
  return role.toLowerCase().replace(/_/g, "-");
}

export function displayRoles(roles: string[]): string[] {
  const seen = new Set<string>();

  return roles
    .map(normalizeRole)
    .filter((role) => role in ROLE_LABELS)
    .filter((role) => {
      if (seen.has(role)) return false;
      seen.add(role);
      return true;
    })
    .map((role) => ROLE_LABELS[role]!);
}
