import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Icon } from "@/components/ui/icon";
import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import type { LabelOption } from "@pkka/domain";
import { ChevronDown } from "lucide-react-native";
import { useState } from "react";
import { Keyboard, Pressable, ScrollView, View } from "react-native";

type SelectFieldProps<T extends string> = {
  value: T | null;
  options: readonly LabelOption<T>[];
  title: string;
  placeholder: string;
  onChange: (value: T) => void;
};

function SelectField<T extends string>({
  value,
  options,
  title,
  placeholder,
  onChange,
}: SelectFieldProps<T>) {
  const [open, setOpen] = useState(false);
  const selected = options.find((option) => option.value === value);

  return (
    <>
      <Pressable
        onPress={() => {
          Keyboard.dismiss();
          setOpen(true);
        }}
        className="border-muted-foreground/25 dark:border-input active:bg-muted bg-background h-12 flex-row items-center justify-between gap-2 rounded-md border px-3 shadow-sm shadow-black/5"
      >
        <Text
          numberOfLines={1}
          className={cn("flex-1 text-sm", selected ? "text-foreground" : "text-muted-foreground")}
        >
          {selected?.label ?? placeholder}
        </Text>
        <Icon as={ChevronDown} className="text-muted-foreground size-4" />
      </Pressable>
      <BottomSheet visible={open} onClose={() => setOpen(false)}>
        <Text variant="h3" className="text-2xl font-bold">
          {title}
        </Text>
        <ScrollView className="-mr-5 mt-4 max-h-80" contentContainerClassName="gap-3 pr-5">
          {options.map((option) => {
            const isSelected = option.value === value;
            return (
              <Pressable
                key={option.value}
                onPress={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
                role="radio"
                accessibilityState={{ checked: isSelected }}
                className={cn(
                  "flex-row items-center gap-3 rounded-lg border px-4 py-3",
                  isSelected ? "border-primary bg-primary/15" : "border-border",
                )}
              >
                <View
                  className={cn(
                    "size-5 items-center justify-center rounded-full border-2",
                    isSelected ? "border-primary" : "border-muted-foreground/40",
                  )}
                >
                  {isSelected ? <View className="bg-primary size-2.5 rounded-full" /> : null}
                </View>
                <Text className="text-foreground flex-1 text-base font-semibold">
                  {option.label}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>
      </BottomSheet>
    </>
  );
}

export { SelectField };
