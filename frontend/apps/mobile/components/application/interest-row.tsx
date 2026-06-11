import { Checkbox } from "@/components/ui/checkbox";
import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { Pressable, View } from "react-native";

type InterestRowProps = {
  label: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
};

function InterestRow({ label, checked, onCheckedChange }: InterestRowProps) {
  return (
    <Pressable
      role="checkbox"
      aria-checked={checked}
      onPress={() => onCheckedChange(!checked)}
      className="flex-row items-center gap-4"
    >
      <Checkbox checked={checked} onCheckedChange={onCheckedChange} />
      <View
        className={cn(
          "flex-1 rounded-md border px-4 py-2.5",
          checked ? "bg-foreground border-foreground" : "bg-background border-foreground",
        )}
      >
        <Text className={cn("text-sm font-bold", checked ? "text-background" : "text-foreground")}>
          {label}
        </Text>
      </View>
    </Pressable>
  );
}

export { InterestRow };
