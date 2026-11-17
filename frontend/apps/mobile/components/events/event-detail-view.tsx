import { ProseContent } from "@/components/content/prose-content";
import { EventTypeBadge } from "@/components/events/event-type-badge";
import { Text } from "@/components/ui/text";
import {
  formatEventDateShort,
  formatSeatsRemaining,
  formatTimeRange,
  isEventRegistrationClosed,
} from "@pkka/domain";
import { useThemeColors } from "@/lib/theme";
import { cn } from "@/lib/utils";
import { type EventDetailsResponse } from "@pkka/api";
import { Image } from "expo-image";
import { Calendar, ImageIcon, MapPin, Users } from "lucide-react-native";
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
  destructive,
}: {
  icon: React.ReactNode;
  label?: string;
  value: string;
  sub?: string;
  destructive?: boolean;
}) {
  return (
    <View className="flex-row items-start gap-3">
      <View
        className={cn(
          "size-10 items-center justify-center rounded-md",
          destructive ? "bg-destructive/10" : "bg-muted",
        )}
      >
        {icon}
      </View>
      <View className="flex-1 gap-0.5">
        {label ? (
          <Text className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
            {label}
          </Text>
        ) : null}
        <Text
          className={cn("text-sm font-bold", destructive ? "text-destructive" : "text-foreground")}
        >
          {value}
        </Text>
        {sub ? <Text className="text-xs text-muted-foreground">{sub}</Text> : null}
      </View>
    </View>
  );
}

export function EventDetailView({ event }: EventDetailViewProps) {
  const theme = useThemeColors();
  const { title, type, startsAt, endsAt, location, seatLimit, seatsTaken, coverImageUrl, tags } =
    event;

  const closed = isEventRegistrationClosed(event);
  const hasImage = !!coverImageUrl && coverImageUrl.startsWith("http");
  const seats = formatSeatsRemaining(seatLimit, seatsTaken);

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
          <EventTypeBadge type={type} className="self-start" />
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

          {location ? (
            <InfoRow
              icon={<MapPin size={18} color={theme.accent} />}
              label="Lokalizacja"
              value={location}
            />
          ) : null}

          {seats && seats.remaining <= 0 ? (
            <InfoRow
              destructive
              icon={<Users size={18} color={theme.destructive} />}
              value="Brak wolnych miejsc"
              sub={`Limit: ${seats.limit} osób${closed ? "" : ", działa lista rezerwowa"}`}
            />
          ) : seats ? (
            <InfoRow
              icon={<Users size={18} color={theme.accent} />}
              value={`Pozostało ${seats.remaining} miejsc`}
              sub={`Limit: ${seats.limit} osób`}
            />
          ) : null}
        </View>

        {event.fullDescription ? (
          <>
            <View className="h-px bg-border" />

            <ProseContent content={event.fullDescription} />
          </>
        ) : null}
      </View>
    </ScrollView>
  );
}
