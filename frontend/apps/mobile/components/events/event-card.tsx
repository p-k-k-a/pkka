import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import { THEME } from "@/lib/theme";
import type { Event } from "@pkka/api";
import { ArrowRight, Link2, MapPin } from "lucide-react-native";
import { View } from "react-native";

type EventCardProps = {
  event: Event;
};

function EventCard({ event }: EventCardProps) {
  const { title, date, format, access, location } = event;
  const isOnline = format === "Online";
  const LocationIcon = isOnline ? Link2 : MapPin;

  return (
    <Card className="border-0 bg-muted gap-3 p-5 shadow-none">
      <View className="flex-row gap-2">
        <Badge variant="outline">
          <Text>{access.toUpperCase()}</Text>
        </Badge>
        <Badge variant={isOnline ? "secondary" : "default"}>
          <Text>{format.toUpperCase()}</Text>
        </Badge>
      </View>

      <View className="gap-1">
        <Text className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          {date}
        </Text>
        <Text variant="h3" className="text-xl font-bold leading-tight">
          {title}
        </Text>
      </View>

      <View className="flex-row items-center gap-2">
        <LocationIcon size={14} color={THEME.light.mutedForeground} />
        <Text className="text-sm text-muted-foreground">{location}</Text>
      </View>

      <View className="flex-row items-center gap-1.5 pt-1">
        <Text className="text-xs font-bold uppercase tracking-widest text-foreground">
          Szczegóły
        </Text>
        <ArrowRight size={14} color={THEME.light.foreground} />
      </View>
    </Card>
  );
}

export { EventCard };
