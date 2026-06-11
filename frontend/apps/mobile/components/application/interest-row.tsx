import { Checkbox } from "@/components/ui/checkbox";
import { Text } from "@/components/ui/text";
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
      <View className="border-foreground bg-background rounded-md border px-4 py-2.5">
        <Text className="text-sm font-bold">{label}</Text>
      </View>
    </Pressable>
  );
}

export { InterestRow };
