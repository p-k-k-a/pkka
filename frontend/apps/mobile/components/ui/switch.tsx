import { cn } from "@/lib/utils";
import { Pressable, View } from "react-native";

type SwitchProps = {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
  accessibilityLabel?: string;
  className?: string;
};

function Switch({
  checked,
  onCheckedChange,
  disabled,
  accessibilityLabel,
  className,
}: SwitchProps) {
  return (
    <Pressable
      role="switch"
      aria-checked={checked}
      aria-disabled={disabled}
      accessibilityLabel={accessibilityLabel}
      disabled={disabled}
      hitSlop={8}
      onPress={() => onCheckedChange(!checked)}
      className={cn(
        "h-7 w-12 shrink-0 justify-center rounded-full border px-0.5",
        checked ? "bg-primary border-primary" : "bg-muted border-border",
        disabled && "opacity-50",
        className,
      )}
    >
      <View
        className={cn(
          "bg-background border-border border-hairline size-6 rounded-full shadow-sm shadow-black/10",
          checked ? "self-end" : "self-start",
        )}
      />
    </Pressable>
  );
}

export { Switch };
