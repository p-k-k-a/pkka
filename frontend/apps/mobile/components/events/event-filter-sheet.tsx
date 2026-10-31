import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { useEffect, useState } from "react";
import { Pressable, View } from "react-native";

export type EventFilters = {
  alumniOnly: boolean;
};

export const EMPTY_EVENT_FILTERS: EventFilters = { alumniOnly: false };

export function countActiveEventFilters(filters: EventFilters) {
  return filters.alumniOnly ? 1 : 0;
}

const AUDIENCE_OPTIONS = [
  { alumniOnly: false, label: "Wszystkie wydarzenia" },
  { alumniOnly: true, label: "Tylko dla alumnów" },
];

type EventFilterSheetProps = {
  visible: boolean;
  value: EventFilters;
  onClose: () => void;
  onApply: (filters: EventFilters) => void;
};

export function EventFilterSheet({ visible, value, onClose, onApply }: EventFilterSheetProps) {
  const [draft, setDraft] = useState(value);

  useEffect(() => {
    if (visible) setDraft(value);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  return (
    <BottomSheet visible={visible} onClose={onClose}>
      <View className="flex-row items-center justify-between">
        <Text variant="h3" className="text-2xl font-bold">
          Filtry
        </Text>
        <Pressable onPress={() => setDraft(EMPTY_EVENT_FILTERS)} hitSlop={8}>
          <Text className="text-accent text-sm font-semibold">Wyczyść</Text>
        </Pressable>
      </View>

      <View className="mt-6 gap-3">
        <Text className="text-muted-foreground text-[10px] font-semibold uppercase tracking-widest">
          Dla kogo
        </Text>
        {AUDIENCE_OPTIONS.map((option) => {
          const selected = draft.alumniOnly === option.alumniOnly;
          return (
            <Pressable
              key={option.label}
              role="radio"
              accessibilityState={{ checked: selected }}
              onPress={() => setDraft((d) => ({ ...d, alumniOnly: option.alumniOnly }))}
              className={cn(
                "flex-row items-center gap-3 rounded-lg border px-4 py-3",
                selected ? "border-accent bg-accent/10" : "border-border",
              )}
            >
              <View
                className={cn(
                  "size-5 items-center justify-center rounded-full border-2",
                  selected ? "border-accent" : "border-muted-foreground/40",
                )}
              >
                {selected ? <View className="bg-accent size-2.5 rounded-full" /> : null}
              </View>
              <Text className="text-foreground text-base">{option.label}</Text>
            </Pressable>
          );
        })}
      </View>

      <Button size="lg" className="mt-6" onPress={() => onApply(draft)}>
        <Text className="font-bold">Pokaż wydarzenia</Text>
      </Button>
    </BottomSheet>
  );
}
