import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { Crown } from "lucide-react-native";
import { View } from "react-native";

export function CoordinatorPill({ label }: { label: string }) {
  const theme = useThemeColors();
  return (
    <View className="border-primary bg-primary/15 flex-row items-center gap-1 rounded-md border px-2 py-0.5">
      <Crown size={12} color={theme.foreground} />
      <Text className="text-foreground text-xs font-semibold">{label}</Text>
    </View>
  );
}

export function Pill({ label }: { label: string }) {
  return (
    <View className="bg-card overflow-hidden rounded-md">
      <View className="bg-muted-foreground/10 px-2 py-1">
        <Text className="text-muted-foreground text-xs font-semibold">{label}</Text>
      </View>
    </View>
  );
}
