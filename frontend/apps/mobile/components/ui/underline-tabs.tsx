import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { Pressable, View } from "react-native";

type UnderlineTabsProps<T extends string> = {
  options: readonly { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
  className?: string;
};

function UnderlineTabs<T extends string>({
  options,
  value,
  onChange,
  className,
}: UnderlineTabsProps<T>) {
  return (
    <View role="tablist" className={cn("border-border flex-row gap-6 border-b", className)}>
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.value}
            role="tab"
            aria-selected={selected}
            hitSlop={8}
            onPress={() => onChange(option.value)}
            className={cn(
              "-mb-px border-b-2 pb-2 pt-1 active:opacity-70",
              selected ? "border-primary" : "border-transparent",
            )}
          >
            <Text
              className={cn(
                "text-sm font-semibold",
                selected ? "text-foreground" : "text-muted-foreground",
              )}
            >
              {option.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export { UnderlineTabs };
