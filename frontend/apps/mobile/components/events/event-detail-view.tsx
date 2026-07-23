import { Badge } from "@/components/ui/badge";
import { Text } from "@/components/ui/text";
import { TYPE_LABELS } from "@/lib/consts";
import { THEME } from "@/lib/theme";
import { formatEventDateShort, formatTimeRange } from "@/lib/utils";
import { EventDetailsDtoType, type EventDetailsDto } from "@pkka/api";
import { Image } from "expo-image";
import { Calendar, ImageIcon, Link2, MapPin, Users } from "lucide-react-native";
import * as React from "react";
import { ScrollView, View } from "react-native";

type EventDetailViewProps = {
  event: EventDetailsDto;
};

function InfoRow({
  icon,
  label,
  value,
  sub,
}: {
  icon: React.ReactNode;
  label?: string;
  value: string;
  sub?: string;
}) {
  return (
    <View className="flex-row items-start gap-3">
      <View className="size-10 items-center justify-center rounded-md bg-muted">{icon}</View>
      <View className="flex-1 gap-0.5">
        {label ? (
          <Text className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
            {label}
          </Text>
        ) : null}
        <Text className="text-sm font-bold text-foreground">{value}</Text>
        {sub ? <Text className="text-xs text-muted-foreground">{sub}</Text> : null}
      </View>
    </View>
  );
}

export function EventDetailView({ event }: EventDetailViewProps) {
  const { title, type, startsAt, endsAt, location, seatLimit, seatsTaken, coverImageUrl, tags } =
    event;

  const isOnline = type === EventDetailsDtoType.ONLINE;
  const hasImage = !!coverImageUrl && coverImageUrl.startsWith("http");
  const seatsLeft = typeof seatLimit === "number" ? seatLimit - seatsTaken : null;

  return (
    <ScrollView
      className="flex-1"
      contentContainerStyle={{ paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
    >
      <View className="gap-6 px-5 pt-5">
        <View className="aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-xl border border-border bg-muted">
          {hasImage ? (
            <Image
              source={{ uri: coverImageUrl }}
              contentFit="cover"
              style={{ width: "100%", height: "100%" }}
            />
          ) : (
            <ImageIcon size={56} color={THEME.light.border} />
          )}
        </View>

        <View className="gap-3">
          <Badge variant="default" className="self-start">
            <Text>{TYPE_LABELS[type].toUpperCase()}</Text>
          </Badge>
          <Text variant="h1" className="text-left text-3xl leading-tight">
            {title}
          </Text>
          {tags.length > 0 ? (
            <View className="flex-row flex-wrap gap-x-3 gap-y-1">
              {tags.map((tag) => (
                <Text key={tag} className="text-sm text-muted-foreground">
                  #{tag}
                </Text>
              ))}
            </View>
          ) : null}
        </View>

        <View className="gap-4">
          <InfoRow
            icon={<Calendar size={18} color={THEME.light.foreground} />}
            value={formatEventDateShort(startsAt)}
            sub={formatTimeRange(startsAt, endsAt)}
          />

          {isOnline ? (
            <InfoRow
              icon={<Link2 size={18} color={THEME.light.foreground} />}
              value="Link do spotkania"
              sub="Link dostępny po zalogowaniu" // TODO: ask client if this should be configurable option in admin panel (maybe we want public events to be joinable only by logged users)
            />
          ) : location ? (
            <InfoRow
              icon={<MapPin size={18} color={THEME.light.foreground} />}
              label="Lokalizacja"
              value={location}
            />
          ) : null}

          {seatsLeft !== null ? (
            <InfoRow
              icon={<Users size={18} color={THEME.light.foreground} />}
              value={`Pozostało ${seatsLeft} miejsc`}
              sub={`Limit: ${seatLimit} osób`}
            />
          ) : null}
        </View>

        <View className="h-px bg-border" />

        <View className="gap-3">
          <Text className="text-xs font-bold uppercase tracking-widest text-foreground">
            O wydarzeniu
          </Text>
          <Text className="leading-7 text-muted-foreground">{event.fullDescription}</Text>
        </View>
      </View>
    </ScrollView>
  );
}
