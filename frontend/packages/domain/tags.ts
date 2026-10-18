export const TAG_NAME_MAX_LENGTH = 32;
export const MAX_TAGS_PER_USER = 20;

export function byTagPopularity(
  a: { name: string; usageCount: number },
  b: { name: string; usageCount: number },
): number {
  return b.usageCount - a.usageCount || a.name.localeCompare(b.name, "pl");
}
