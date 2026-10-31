import { EventTypeBadge } from "@/components/events/event-type-badge";
import { Text } from "@/components/ui/text";
import {
  formatEventDateLong,
  formatSeatsCompact,
  registrationStatusLabelUpper,
} from "@pkka/domain";
import { useThemeColors } from "@/lib/theme";
import { EventType, type EventListItemResponse } from "@pkka/api";
import { Link } from "expo-router";
import { ArrowRight, Calendar, Link2, MapPin, Users } from "lucide-react-native";
import { Pressable, View } from "react-native";

type EventCardProps = {
  event: EventListItemResponse;
};

function EventCard({ event }: EventCardProps) {
  const theme = useThemeColors();
  const { slug, title, startsAt, type, location, seatLimit, seatsTaken, registrationStatus } =
    event;
  const seatsLabel = formatSeatsCompact(seatLimit, seatsTaken);
  const isOnline = type === EventType.ONLINE;
  const LocationIcon = isOnline ? Link2 : MapPin;

  const card = (
    <View className="bg-muted gap-5 rounded-2xl p-6">
      <View className="flex-row flex-wrap items-center gap-2">
        <EventTypeBadge type={type} />
        {registrationStatus ? (
          <View className="border-muted-foreground/25 dark:border-input rounded-lg border px-3 py-1">
            <Text className="text-foreground text-[11px] font-semibold uppercase tracking-widest">
              {registrationStatusLabelUpper(registrationStatus)}
            </Text>
          </View>
        ) : null}
      </View>

      <Text
        role="heading"
        aria-level="3"
        className="font-heading text-foreground text-lg font-semibold leading-snug"
      >
        {title}
      </Text>

      <View className="gap-3">
        <View className="flex-row items-start gap-3">
          <Calendar size={16} color={theme.mutedForeground} style={{ marginTop: 2 }} />
          <Text className="text-muted-foreground flex-1 text-sm">
            {formatEventDateLong(startsAt)}
          </Text>
        </View>
        {location ? (
          <View className="flex-row items-start gap-3">
            <LocationIcon size={16} color={theme.mutedForeground} style={{ marginTop: 2 }} />
            <Text className="text-muted-foreground flex-1 text-sm">{location}</Text>
          </View>
        ) : null}
        {seatsLabel ? (
          <View className="flex-row items-start gap-3">
            <Users size={16} color={theme.mutedForeground} style={{ marginTop: 2 }} />
            <Text className="text-muted-foreground flex-1 text-sm">{seatsLabel}</Text>
          </View>
        ) : null}
      </View>

      <View className="bg-background flex-row items-center gap-1.5 self-end rounded-lg px-3 py-2">
        <Text className="text-accent text-sm font-medium">Szczegóły</Text>
        <ArrowRight size={14} color={theme.accent} />
      </View>
    </View>
  );

  return (
    <Link href={{ pathname: "/events/[slug]", params: { slug } }} asChild>
      <Pressable className="active:opacity-90">{card}</Pressable>
    </Link>
  );
}

export { EventCard };
