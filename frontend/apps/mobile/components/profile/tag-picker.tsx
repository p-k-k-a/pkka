import { SearchMultiSelect, type SelectOption } from "@/components/ui/search-multi-select";
import { Text } from "@/components/ui/text";
import type { AlumniTagResponse } from "@pkka/api";
import { TAG_NAME_MAX_LENGTH } from "@pkka/domain";

type TagPickerProps = {
  catalog: AlumniTagResponse[];
  value: string[];
  onChange: (names: string[] | ((prev: string[]) => string[])) => void;
  loading?: boolean;
  error?: boolean;
};

export function TagPicker({
  catalog,
  value,
  onChange,
  loading = false,
  error = false,
}: TagPickerProps) {
  const selectedByLower = new Map(value.map((name) => [name.toLowerCase(), name]));
  const known = catalog.map((tag) => selectedByLower.get(tag.name.toLowerCase()) ?? tag.name);
  const added = value.filter((name) => !known.includes(name));
  const options: SelectOption[] = [...known, ...added].map((name) => ({ id: name, label: name }));

  if (loading) {
    return <Text className="text-muted-foreground text-sm">Ładowanie umiejętności...</Text>;
  }

  if (error) {
    return (
      <Text className="text-destructive text-sm font-medium">
        Nie udało się załadować listy umiejętności. Spróbuj ponownie później.
      </Text>
    );
  }

  return (
    <SearchMultiSelect
      options={options}
      value={value}
      onChange={onChange}
      onCreate={(name) => onChange((prev) => [...prev, name])}
      maxLength={TAG_NAME_MAX_LENGTH}
      placeholder="Wyszukaj lub dodaj..."
      uppercaseChips
    />
  );
}
