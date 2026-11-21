import { Button } from "@/components/ui/button";
import { ParticipationMenu } from "@/components/events/participation-menu";
import { Text } from "@/components/ui/text";
import { Icon } from "@/components/ui/icon";
import { useAuth } from "@/lib/auth-context";
import { openWithFallback } from "@/lib/contact";
import {
  getGetEventBySlugQueryKey,
  getListEventsQueryKey,
  useCancelEventRegistration,
  useRegisterForEvent,
  EventRegistrationStatus,
  EventType,
  type EventDetailsResponse,
} from "@pkka/api";
import {
  eventCalendarLocation,
  eventRegistrationErrorMessage,
  hasOnlineLink,
  isEventFull,
  isEventPast,
  isEventRegistrationClosed,
} from "@pkka/domain";
import { useQueryClient } from "@tanstack/react-query";
import * as Calendar from "expo-calendar";
import { router } from "expo-router";
import { CalendarPlus, Check, Clock, LogOut, Video } from "lucide-react-native";
import { useState } from "react";
import { View } from "react-native";

function waitlistDescription(event: EventDetailsResponse) {
  const base = "Dostaniesz miejsce, gdy ktoś zrezygnuje.";
  if (!hasOnlineLink(event.type)) return base;
  if (event.transmissionUrl)
    return `${base} Do tego czasu możesz uczestniczyć w wydarzeniu online.`;
  return event.type === EventType.ONLINE
    ? `${base} Link do spotkania dostaniesz, gdy zwolni się dla Ciebie miejsce.`
    : `${base} Link do spotkania pojawi się, gdy organizator go doda.`;
}

function JoinOnlineMeeting({ url, label = "Dołącz" }: { url: string; label?: string }) {
  return (
    <View className="flex-1">
      <Button onPress={() => void openWithFallback(url, url)}>
        <Icon as={Video} size={18} />
        <Text>{label}</Text>
      </Button>
    </View>
  );
}

export function EventParticipationFooter({ event }: { event: EventDetailsResponse }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: getGetEventBySlugQueryKey(event.slug) }),
      queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() }),
    ]);

  const register = useRegisterForEvent({ mutation: { onSettled: refresh } });
  const unregister = useCancelEventRegistration({ mutation: { onSettled: refresh } });
  const pending = register.isPending || unregister.isPending;
  const waitlisted = event.registrationStatus === EventRegistrationStatus.WAITLISTED;
  const full = isEventFull(event.seatLimit, event.seatsTaken);
  const closed = isEventRegistrationClosed(event);
  const online = hasOnlineLink(event.type);
  const meetingUrl = online ? event.transmissionUrl : undefined;
  const onlineOnly = waitlisted && Boolean(meetingUrl);

  const onJoin = async () => {
    setError(null);
    try {
      await register.mutateAsync({ eventId: event.id });
    } catch (e) {
      setError(eventRegistrationErrorMessage(e, "join"));
    }
  };

  const onLeave = async () => {
    setError(null);
    try {
      await unregister.mutateAsync({ eventId: event.id });
    } catch (e) {
      setError(eventRegistrationErrorMessage(e, "leave"));
    }
  };

  const onAddToCalendar = async () => {
    try {
      const { granted } = await Calendar.requestCalendarPermissionsAsync();
      if (!granted) {
        setError("Brak dostępu do kalendarza.");
        return;
      }
      await Calendar.createEventInCalendarAsync({
        title: event.title,
        startDate: new Date(event.startsAt),
        endDate: new Date(event.endsAt),
        location: eventCalendarLocation(onlineOnly ? undefined : event.location, meetingUrl),
        notes: meetingUrl
          ? [`Link do spotkania: ${meetingUrl}`, event.fullDescription].filter(Boolean).join("\n\n")
          : event.fullDescription,
      });
    } catch {
      setError("Nie udało się otworzyć kalendarza.");
    }
  };

  const ended = isEventPast(event.endsAt);
  if (ended && !error) return null;

  return (
    <View className="border-border bg-card gap-3 border-t px-5 py-4">
      {error ? (
        <Text className="text-destructive dark:text-foreground text-xs font-semibold">{error}</Text>
      ) : null}

      {ended ? null : !user ? (
        <>
          <Text className="text-muted-foreground text-center text-[10px] font-semibold uppercase tracking-widest">
            Niezalogowani użytkownicy nie mogą dołączyć do wydarzenia
          </Text>
          <Button className="w-full" onPress={() => router.push("/login")}>
            <Text>Zaloguj się, aby dołączyć</Text>
          </Button>
        </>
      ) : waitlisted ? (
        <View className="flex-row gap-3">
          <ParticipationMenu
            label="W kolejce"
            icon={Clock}
            disabled={pending}
            header={{
              title: "Jesteś na liście rezerwowej",
              description: waitlistDescription(event),
            }}
            items={[
              ...(onlineOnly
                ? [{ label: "Dodaj do kalendarza", icon: CalendarPlus, onPress: onAddToCalendar }]
                : []),
              {
                label: "Opuść listę rezerwową",
                icon: LogOut,
                destructive: true,
                onPress: () => void onLeave(),
              },
            ]}
          />
          {onlineOnly && meetingUrl ? (
            <JoinOnlineMeeting url={meetingUrl} label="Dołącz online" />
          ) : null}
        </View>
      ) : event.registrationStatus ? (
        <>
          {online && !meetingUrl ? (
            <Text className="text-muted-foreground text-center text-xs">
              Link do spotkania pojawi się tutaj, gdy organizator go doda.
            </Text>
          ) : null}
          <View className="flex-row gap-3">
            <ParticipationMenu
              label="Zapisany"
              icon={Check}
              disabled={pending}
              items={[
                { label: "Dodaj do kalendarza", icon: CalendarPlus, onPress: onAddToCalendar },
                {
                  label: "Opuść wydarzenie",
                  icon: LogOut,
                  destructive: true,
                  onPress: () => void onLeave(),
                },
              ]}
            />
            {meetingUrl ? <JoinOnlineMeeting url={meetingUrl} /> : null}
          </View>
        </>
      ) : closed ? (
        <Button className="w-full" disabled>
          <Text>Zapisy zamknięte</Text>
        </Button>
      ) : (
        <Button className="w-full" disabled={pending} onPress={onJoin}>
          <Text>
            {pending
              ? "Zapisywanie..."
              : full
                ? "Zapisz się na listę rezerwową"
                : "Dołącz do wydarzenia"}
          </Text>
        </Button>
      )}
    </View>
  );
}
