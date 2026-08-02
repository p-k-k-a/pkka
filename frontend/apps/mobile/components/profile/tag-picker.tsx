import { SearchMultiSelect, type SelectOption } from "@/components/ui/search-multi-select";
import { Text } from "@/components/ui/text";
import type { UserTagResponse } from "@pkka/api";
import { useMemo } from "react";

const MAX_TAGS = 20;

type TagPickerProps = {
  options: UserTagResponse[];
  value: string[];
  onChange: (ids: string[] | ((prev: string[]) => string[])) => void;
  loading?: boolean;
  error?: boolean;
  max?: number;
};

export function TagPicker({
  options,
  value,
  onChange,
  loading = false,
  error = false,
  max = MAX_TAGS,
}: TagPickerProps) {
  const selectOptions = useMemo<SelectOption[]>(
    () => options.map((tag) => ({ id: tag.id, label: tag.name })),
    [options],
  );

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
      options={selectOptions}
      value={value}
      onChange={onChange}
      placeholder="Wyszukaj umiejętności..."
      max={max}
      maxHint={`Możesz wybrać maksymalnie ${max} umiejętności.`}
      uppercaseChips
    />
  );
}
