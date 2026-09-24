import { Text } from "@/components/ui/text";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";

type OptionChipsProps = {
  options: readonly { value: string; label: string }[];
  value: string[];
  onChange: (value: string[]) => void;
};

function OptionChips({ options, value, onChange }: OptionChipsProps) {
  return (
    <ToggleGroup
      type="multiple"
      variant="outline"
      value={value}
      onValueChange={onChange}
      className="items-stretch"
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
              selected ? "bg-primary/15 border-primary active:bg-primary/25" : "active:bg-muted",
            )}
          >
            <Text className={cn("text-center text-[13px] font-bold leading-4", "text-foreground")}>
              {option.label}
            </Text>
          </ToggleGroupItem>
        );
      })}
    </ToggleGroup>
  );
}

export { OptionChips };
