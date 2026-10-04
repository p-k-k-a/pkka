// Polish picks the noun form from the last digits: 1 → one, 2–4 (but not 12–14) → few, everything else → many.
export function pluralPl(count: number, one: string, few: string, many: string) {
  const abs = Math.abs(count);
  if (abs === 1) return one;
  const lastDigit = abs % 10;
  const lastTwo = abs % 100;
  if (lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)) return few;
  return many;
}
