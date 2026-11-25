import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { View } from "react-native";

export function Pill({ label, amber }: { label: string; amber?: boolean }) {
  return (
    <View
      className={cn("rounded-md px-2 py-1", amber ? "bg-primary/15" : "bg-muted-foreground/10")}
    >
      <Text
        className={cn(
          "text-xs font-semibold",
          amber ? "text-secondary-foreground" : "text-muted-foreground",
        )}
      >
        {label}
      </Text>
    </View>
  );
}
