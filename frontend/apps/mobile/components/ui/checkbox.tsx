import { cn } from "@/lib/utils";
import { useTheme } from "@react-navigation/native";
import { Check } from "lucide-react-native";
import { Pressable } from "react-native";

type CheckboxProps = {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  className?: string;
};

function Checkbox({ checked, onCheckedChange, className }: CheckboxProps) {
  const { colors } = useTheme();

  return (
    <Pressable
      role="checkbox"
      aria-checked={checked}
      hitSlop={8}
      onPress={() => onCheckedChange(!checked)}
      className={cn(
        "border-foreground bg-background size-6 shrink-0 items-center justify-center rounded-sm border-2",
        checked && "bg-foreground",
        className,
      )}
    >
      {checked ? <Check size={15} strokeWidth={3.5} color={colors.background} /> : null}
    </Pressable>
  );
}

export { Checkbox };
