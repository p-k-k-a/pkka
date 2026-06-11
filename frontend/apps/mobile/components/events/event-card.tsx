import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import { TYPE_LABELS } from "@/lib/consts";
import { THEME } from "@/lib/theme";
import { formatEventDateLong } from "@/lib/utils";
import { EventListItemDtoType, type EventListItemDto } from "@pkka/api";
import { Link } from "expo-router";
import { ArrowRight, Link2, MapPin } from "lucide-react-native";
import { Pressable, View } from "react-native";

type EventCardProps = {
  event: EventListItemDto;
};

function EventCard({ event }: EventCardProps) {
  const { id, title, startsAt, type, location, seatLimit, seatsTaken } = event;
  const isOnline = type === EventListItemDtoType.ONLINE;
  const LocationIcon = isOnline ? Link2 : MapPin;

  const card = (
    <Card className="border-0 bg-muted gap-3 p-5 shadow-none">
      <View className="gap-1">
        <Text className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          {formatEventDateLong(startsAt)}
        </Text>
        <Text variant="h3" className="text-xl font-bold leading-tight">
          {title}
        </Text>
      </View>

      {location ? (
        <View className="flex-row items-center gap-2">
          <LocationIcon size={14} color={THEME.light.mutedForeground} />
          <Text className="text-sm text-muted-foreground">{location}</Text>
        </View>
      ) : null}

      <View className="flex-row items-center gap-1.5 pt-1">
        <Text className="text-xs font-bold uppercase tracking-widest text-foreground">
          Szczegóły
        </Text>
        <ArrowRight size={14} color={THEME.light.foreground} />
      </View>

      <View className="flex-row gap-2">
        <Badge variant="default">
          <Text>{TYPE_LABELS[type!].toUpperCase()}</Text>
        </Badge>
        {typeof seatLimit === "number" ? (
          <Badge variant="outline">
            <Text>{`${seatsTaken!}/${seatLimit} MIEJSC`}</Text>
          </Badge>
        ) : null}
      </View>
    </Card>
  );

  return (
    <Link href={{ pathname: "/events/[id]", params: { id: id! } }} asChild>
      <Pressable className="active:opacity-90">{card}</Pressable>
    </Link>
  );
}

export { EventCard };
