const TAG_NAME_MAX_LENGTH = 32;
const TAG_NAME_PATTERN = /^(?=.*[\p{L}\p{N}])[\p{L}\p{N}+#./ -]+$/u;

export function normalizeTagName(raw: string): string {
  return raw.trim().replace(/\s+/g, " ");
}

export function tagNameKey(name: string): string {
  return name.toLowerCase();
}

export function tagNameError(name: string): string | null {
  if (name.length > TAG_NAME_MAX_LENGTH) {
    return `Nazwa może mieć najwyżej ${TAG_NAME_MAX_LENGTH} znaki.`;
  }
  if (!TAG_NAME_PATTERN.test(name)) {
    return "Dozwolone są litery, cyfry, spacje oraz znaki + # . - /";
  }
  return null;
}

export function byTagPopularity(
  a: { name: string; usageCount: number },
  b: { name: string; usageCount: number },
): number {
  return b.usageCount - a.usageCount || a.name.localeCompare(b.name, "pl");
}
