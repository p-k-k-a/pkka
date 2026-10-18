import { SearchMultiSelect, type SelectOption } from "@/components/ui/search-multi-select";
import { Text } from "@/components/ui/text";
import type { UserTagCatalogResponse } from "@pkka/api";
import { TAG_NAME_MAX_LENGTH, byTagPopularity } from "@pkka/domain";
import { useMemo } from "react";

type TagPickerProps = {
  catalog: UserTagCatalogResponse[];
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
  const options = useMemo<SelectOption[]>(() => {
    const selectedByLower = new Map(value.map((name) => [name.toLowerCase(), name]));
    const known = [...catalog]
      .sort(byTagPopularity)
      .map((tag) => selectedByLower.get(tag.name.toLowerCase()) ?? tag.name);
    const added = value.filter((name) => !known.includes(name));
    return [...known, ...added].map((name) => ({ id: name, label: name }));
  }, [catalog, value]);

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
