import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { SORT_OPTIONS, type SortOption } from "@/lib/alumni-directory";
import { cn } from "@/lib/utils";
import { useEffect, useState } from "react";
import { Pressable, View } from "react-native";

type SortSheetProps = {
  visible: boolean;
  value: SortOption;
  onClose: () => void;
  onApply: (value: SortOption) => void;
};

export function SortSheet({ visible, value, onClose, onApply }: SortSheetProps) {
  const [draft, setDraft] = useState<SortOption>(value);

  // Seed the draft from the applied sort only on open (see FilterSheet).
  useEffect(() => {
    if (visible) setDraft(value);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  return (
    <BottomSheet visible={visible} onClose={onClose}>
      <View className="gap-1">
        <Text variant="h3" className="text-2xl font-bold">
          Sortuj według
        </Text>
        <Text className="text-muted-foreground text-sm">
          Wybierz preferowaną kolejność wyświetlania listy alumnów.
        </Text>
      </View>

      <View className="my-4">
        {SORT_OPTIONS.map((option) => {
          const selected = draft === option.value;
          return (
            <Pressable
              key={option.value}
              onPress={() => setDraft(option.value)}
              role="radio"
              accessibilityState={{ selected }}
              className="border-border flex-row items-center justify-between border-b py-4"
            >
              <Text className="text-foreground text-base">{option.label}</Text>
              <View
                className={cn(
                  "size-5 items-center justify-center rounded-full border-2",
                  selected ? "border-primary" : "border-muted-foreground/40",
                )}
              >
                {selected ? <View className="bg-primary size-2.5 rounded-full" /> : null}
              </View>
            </Pressable>
          );
        })}
      </View>

      <View className="gap-3">
        <Button size="lg" onPress={() => onApply(draft)}>
          <Text className="font-bold">Zastosuj</Text>
        </Button>
        <Pressable onPress={onClose} className="items-center py-1">
          <Text className="text-muted-foreground text-sm font-semibold">Anuluj</Text>
        </Pressable>
      </View>
    </BottomSheet>
  );
}
