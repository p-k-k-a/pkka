import { Text } from "@/components/ui/text";
import { eventTypeLabelUpper, formatEventDateShort, formatTimeRange } from "@pkka/domain";
import { useThemeColors } from "@/lib/theme";
import { EventType, type EventDetailsResponse } from "@pkka/api";
import { Image } from "expo-image";
import { Calendar, ImageIcon, Link2, MapPin, Users } from "lucide-react-native";
import * as React from "react";
import { ScrollView, View } from "react-native";

type EventDetailViewProps = {
  event: EventDetailsResponse;
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
  const theme = useThemeColors();
  const { title, type, startsAt, endsAt, location, seatLimit, seatsTaken, coverImageUrl, tags } =
    event;

  const isOnline = type === EventType.ONLINE;
  const hasImage = !!coverImageUrl && coverImageUrl.startsWith("http");
  const seatsLeft = typeof seatLimit === "number" ? seatLimit - seatsTaken! : null;

  return (
    <ScrollView
      className="flex-1"
      contentContainerStyle={{ paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
    >
      <View className="gap-6 px-5 pt-5">
        <View className="aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-2xl border border-border bg-muted">
          {hasImage ? (
            <Image
              source={{ uri: coverImageUrl }}
              contentFit="cover"
              style={{ width: "100%", height: "100%" }}
            />
          ) : (
            <ImageIcon size={56} color={theme.mutedForeground} strokeWidth={1.25} />
          )}
        </View>

        <View className="gap-3">
          <View className="bg-band dark:bg-muted self-start rounded-lg px-3 py-1">
            <Text className="text-band-foreground text-[11px] font-semibold uppercase tracking-widest">
              {eventTypeLabelUpper(type)}
            </Text>
          </View>
          <Text
            role="heading"
            aria-level="1"
            className="font-heading text-foreground text-[28px] font-semibold leading-tight tracking-tight"
          >
            {title}
          </Text>
          {(tags ?? []).length > 0 ? (
            <View className="flex-row flex-wrap gap-x-3 gap-y-1">
              {(tags ?? []).map((tag) => (
                <Text key={tag} className="text-accent text-sm font-medium">
                  #{tag}
                </Text>
              ))}
            </View>
          ) : null}
        </View>

        <View className="gap-4">
          <InfoRow
            icon={<Calendar size={18} color={theme.accent} />}
            value={formatEventDateShort(startsAt)}
            sub={formatTimeRange(startsAt, endsAt)}
          />

          {isOnline ? (
            <InfoRow
              icon={<Link2 size={18} color={theme.accent} />}
              value="Link do spotkania"
              sub="Link dostępny po zalogowaniu" // TODO: ask client if this should be configurable option in admin panel (maybe we want public events to be joinable only by logged users)
            />
          ) : location ? (
            <InfoRow
              icon={<MapPin size={18} color={theme.accent} />}
              label="Lokalizacja"
              value={location}
            />
          ) : null}

          {seatsLeft !== null ? (
            <InfoRow
              icon={<Users size={18} color={theme.accent} />}
              value={`Pozostało ${seatsLeft} miejsc`}
              sub={`Limit: ${seatLimit} osób`}
            />
          ) : null}
        </View>

        <View className="h-px bg-border" />

        <View className="gap-3">
          <Text className="font-heading text-foreground text-xl font-semibold">O wydarzeniu</Text>
          <Text className="text-foreground/90 text-base leading-7">{event.fullDescription}</Text>
        </View>
      </View>
    </ScrollView>
  );
}
