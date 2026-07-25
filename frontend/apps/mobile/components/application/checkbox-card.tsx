import { Checkbox } from "@/components/ui/checkbox";
import { Text } from "@/components/ui/text";
import { Pressable, View } from "react-native";

type CheckboxCardProps = {
  title: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
};

function CheckboxCard({ title, description, checked, onCheckedChange }: CheckboxCardProps) {
  return (
    <Pressable
      role="checkbox"
      aria-checked={checked}
      onPress={() => onCheckedChange(!checked)}
      className="border-foreground bg-background flex-row items-start gap-4 rounded-md border p-4"
    >
      <Checkbox checked={checked} onCheckedChange={onCheckedChange} className="mt-0.5" />
      <View className="flex-1 gap-1">
        <Text className="text-sm font-extrabold uppercase leading-5 tracking-wide">{title}</Text>
        <Text className="text-muted-foreground text-sm leading-5">{description}</Text>
      </View>
    </Pressable>
  );
}

export { CheckboxCard };
