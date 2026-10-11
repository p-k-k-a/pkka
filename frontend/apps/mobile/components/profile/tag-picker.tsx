import { SearchMultiSelect, type SelectOption } from "@/components/ui/search-multi-select";
import { Text } from "@/components/ui/text";
import type { UserTagCatalogResponse } from "@pkka/api";
import { byTagPopularity, tagNameError, tagNameKey } from "@pkka/domain";
import { useMemo } from "react";

const MAX_TAGS = 20;
const IDLE_LIMIT = 20;

type TagPickerProps = {
  catalog: UserTagCatalogResponse[];
  /** Selected tag names; a name missing from the catalog is created on save. */
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
    const known = [...catalog]
      .sort(byTagPopularity)
      .map((tag) => ({ id: tagNameKey(tag.name), label: tag.name }));
    const knownKeys = new Set(known.map((option) => option.id));
    const added = value
      .filter((name) => !knownKeys.has(tagNameKey(name)))
      .map((name) => ({ id: tagNameKey(name), label: name }));
    return [...known, ...added];
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
      value={value.map(tagNameKey)}
      onChange={(next) =>
        onChange((prev) => {
          const labels = new Map(options.map((option) => [option.id, option.label]));
          prev.forEach((name) => labels.set(tagNameKey(name), name));
          const keys = typeof next === "function" ? next(prev.map(tagNameKey)) : next;
          return keys.map((key) => labels.get(key) ?? key);
        })
      }
      onCreate={(name) =>
        onChange((prev) =>
          prev.length >= MAX_TAGS ||
          prev.some((existing) => tagNameKey(existing) === tagNameKey(name))
            ? prev
            : [...prev, name],
        )
      }
      createError={tagNameError}
      placeholder="Wyszukaj lub dodaj..."
      max={MAX_TAGS}
      maxHint={`Możesz wybrać maksymalnie ${MAX_TAGS} umiejętności.`}
      idleLimit={IDLE_LIMIT}
      uppercaseChips
    />
  );
}
