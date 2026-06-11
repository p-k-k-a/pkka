import { Text } from "@/components/ui/text";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";

type OptionChipsProps = {
  options: readonly string[];
  value: string[];
  onChange: (value: string[]) => void;
};

function OptionChips({ options, value, onChange }: OptionChipsProps) {
  return (
    <ToggleGroup type="multiple" variant="outline" value={value} onValueChange={onChange}>
      {options.map((option, index) => {
        const selected = value.includes(option);
        return (
          <ToggleGroupItem
            key={option}
            value={option}
            isFirst={index === 0}
            isLast={index === options.length - 1}
            className={cn(
              selected
                ? "bg-foreground border-foreground active:bg-foreground/80"
                : "active:bg-muted",
            )}
          >
            <Text
              className={cn("text-sm font-bold", selected ? "text-background" : "text-foreground")}
            >
              {option}
            </Text>
          </ToggleGroupItem>
        );
      })}
    </ToggleGroup>
  );
}

export { OptionChips };
