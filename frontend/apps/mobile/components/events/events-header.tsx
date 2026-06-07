import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { View } from "react-native";

type EventsHeaderProps = {
  label: string;
  title: string;
  intro: string;
  className?: string;
};

function EventsHeader({ label, title, intro, className }: EventsHeaderProps) {
  return (
    <View className={cn("gap-4", className)}>
      <View className="gap-1">
        <Text className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          {label}
        </Text>
        <Text variant="h1" className="text-left text-4xl">
          {title}
        </Text>
      </View>
      <Text className="leading-6 text-muted-foreground">{intro}</Text>
    </View>
  );
}

export { EventsHeader };
