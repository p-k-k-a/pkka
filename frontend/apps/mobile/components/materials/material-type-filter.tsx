import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import type { MaterialType } from "@pkka/api";
import { MATERIAL_TYPE_OPTIONS } from "@pkka/domain";
import { Pressable, ScrollView } from "react-native";

const OPTIONS: { value: MaterialType | null; label: string }[] = [
  { value: null, label: "Wszystkie" },
  ...MATERIAL_TYPE_OPTIONS,
];

type MaterialTypeFilterProps = {
  value: MaterialType | null;
  onChange: (value: MaterialType | null) => void;
};

export function MaterialTypeFilter({ value, onChange }: MaterialTypeFilterProps) {
  return (
    <ScrollView
      horizontal
      role="radiogroup"
      showsHorizontalScrollIndicator={false}
      keyboardShouldPersistTaps="handled"
      className="-mx-5 grow-0"
      contentContainerClassName="gap-2 px-5"
    >
      {OPTIONS.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.value ?? "all"}
            role="radio"
            accessibilityState={{ checked: selected }}
            onPress={() => onChange(option.value)}
            className={cn(
              "rounded-lg border px-3 py-2",
              selected
                ? "bg-primary/15 border-primary"
                : "border-muted-foreground/25 dark:border-input active:bg-muted",
            )}
          >
            <Text className="text-foreground text-[13px] font-semibold">{option.label}</Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}
