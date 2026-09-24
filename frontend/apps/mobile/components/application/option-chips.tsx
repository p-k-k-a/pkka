import { Text } from "@/components/ui/text";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";
import type { LabelOption } from "@pkka/domain";

type OptionChipsProps<T extends string> = {
  options: readonly LabelOption<T>[];
  value: T[];
  onChange: (value: T[]) => void;
};

function OptionChips<T extends string>({ options, value, onChange }: OptionChipsProps<T>) {
  return (
    <ToggleGroup
      type="multiple"
      variant="outline"
      value={value}
      onValueChange={(next) => onChange(next as T[])}
    >
      {options.map((option, index) => {
        const selected = value.includes(option.value);
        return (
          <ToggleGroupItem
            key={option.value}
            value={option.value}
            isFirst={index === 0}
            isLast={index === options.length - 1}
            className={cn(
              "h-auto min-h-10 flex-1 shrink px-2 py-2",
              selected ? "bg-secondary border-primary active:bg-secondary/80" : "active:bg-muted",
            )}
          >
            <Text
              className={cn(
                "text-center text-[13px] font-bold leading-4",
                selected ? "text-secondary-foreground" : "text-foreground",
              )}
            >
              {option.label}
            </Text>
          </ToggleGroupItem>
        );
      })}
    </ToggleGroup>
  );
}

export { OptionChips };
