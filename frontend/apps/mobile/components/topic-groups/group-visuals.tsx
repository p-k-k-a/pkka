import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { avatarInitials } from "@pkka/domain";
import { View } from "react-native";

const TILES = [
  { bg: "bg-primary/15", text: "text-secondary-foreground" },
  { bg: "bg-accent/15", text: "text-accent" },
  { bg: "bg-muted dark:bg-input", text: "text-muted-foreground" },
  { bg: "bg-band", text: "text-band-foreground" },
];

export function GroupTile({ name }: { name: string }) {
  const tile = TILES[[...name].reduce((sum, char) => sum + char.charCodeAt(0), 0) % TILES.length]!;
  return (
    <View className={cn("size-11 items-center justify-center rounded-xl", tile.bg)}>
      <Text className={cn("font-heading text-base font-semibold", tile.text)}>
        {avatarInitials(name)}
      </Text>
    </View>
  );
}

export function Pill({ label, amber }: { label: string; amber?: boolean }) {
  return (
    <View
      className={cn("rounded-md px-2 py-1", amber ? "bg-primary/15" : "bg-muted dark:bg-input")}
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
