import { cn } from "@/lib/utils";
import { useThemeColors } from "@/lib/theme";
import { Check } from "lucide-react-native";
import { Pressable } from "react-native";

type CheckboxProps = {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  className?: string;
};

function Checkbox({ checked, onCheckedChange, className }: CheckboxProps) {
  const theme = useThemeColors();

  return (
    <Pressable
      role="checkbox"
      aria-checked={checked}
      hitSlop={8}
      onPress={() => onCheckedChange(!checked)}
      className={cn(
        "border-muted-foreground bg-background size-6 shrink-0 items-center justify-center rounded-md border-2",
        checked && "border-primary bg-primary",
        className,
      )}
    >
      {checked ? <Check size={15} strokeWidth={3.5} color={theme.primaryForeground} /> : null}
    </Pressable>
  );
}

export { Checkbox };
