const rules = new Intl.PluralRules("pl-PL");

export function pluralPl(count: number, one: string, few: string, many: string) {
  const form = rules.select(count);
  return form === "one" ? one : form === "few" ? few : many;
}
