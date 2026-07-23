import { Checkbox } from "@/components/ui/checkbox";
import { Text } from "@/components/ui/text";
import { Pressable, View } from "react-native";

type ConsentRowProps = {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  children: React.ReactNode;
};

function ConsentRow({ checked, onCheckedChange, children }: ConsentRowProps) {
  return (
    <View className="flex-row items-start gap-4">
      <Checkbox checked={checked} onCheckedChange={onCheckedChange} className="mt-0.5" />
      <Pressable
        role="checkbox"
        aria-checked={checked}
        onPress={() => onCheckedChange(!checked)}
        className="flex-1"
      >
        <Text className="text-foreground text-sm font-semibold leading-6">{children}</Text>
      </Pressable>
    </View>
  );
}

export { ConsentRow };
