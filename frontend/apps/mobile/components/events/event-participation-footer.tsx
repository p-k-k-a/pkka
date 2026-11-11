import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { DISCORD_URL, KEYCLOAK_URL } from "@/lib/auth-config";
import { useAuth } from "@/lib/auth-context";
import { forgetSignUpSource, savedSignUpSource } from "@/lib/signup-source";
import { useThemeColors } from "@/lib/theme";
import { DiscordIcon } from "@pkka/icons/native";
import {
  getGetEventBySlugQueryKey,
  getListEventsQueryKey,
  useCancelEventRegistration,
  useRegisterForEvent,
  EventRegistrationStatus,
  type EventDetailsResponse,
} from "@pkka/api";
import { eventRegistrationErrorMessage, isEventFull } from "@pkka/domain";
import { useQueryClient } from "@tanstack/react-query";
import * as Calendar from "expo-calendar";
import { useState } from "react";
import { View } from "react-native";

type ActiveSheet = "calendar" | "leave" | "login" | null;

export function EventParticipationFooter({ event }: { event: EventDetailsResponse }) {
  const { user, loginBrowser } = useAuth();
  const theme = useThemeColors();
  const queryClient = useQueryClient();
  const [sheet, setSheet] = useState<ActiveSheet>(null);
  const [error, setError] = useState<string | null>(null);

  const register = useRegisterForEvent();
  const unregister = useCancelEventRegistration();
  const waitlisted = event.registrationStatus === EventRegistrationStatus.WAITLISTED;
  const full = isEventFull(event.seatLimit, event.seatsTaken);
  const pending = register.isPending || unregister.isPending;

  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: getGetEventBySlugQueryKey(event.slug) }),
      queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() }),
    ]);

  const onJoin = async () => {
    setError(null);
    try {
      const response = await register.mutateAsync({
        eventId: event.id,
        params: { source: savedSignUpSource(event.slug) },
      });
      forgetSignUpSource(event.slug);
      await refresh();
      if (response.status === 201 && response.data.status === EventRegistrationStatus.REGISTERED) {
        setSheet("calendar");
      }
    } catch (e) {
      setError(eventRegistrationErrorMessage(e, "join"));
      await refresh();
    }
  };

  const onLeave = async () => {
    setError(null);
    try {
      await unregister.mutateAsync({ eventId: event.id });
      setSheet(null);
      await refresh();
    } catch (e) {
      setSheet(null);
      setError(eventRegistrationErrorMessage(e, "leave"));
      await refresh();
    }
  };

  const onLogin = async (authorizationUrl: string) => {
    setSheet(null);
    await loginBrowser(authorizationUrl);
    await refresh();
  };

  const onAddToCalendar = async () => {
    setSheet(null);
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
        location: event.location ?? undefined,
        notes: event.fullDescription ?? undefined,
      });
    } catch {
      setError("Nie udało się otworzyć kalendarza.");
    }
  };

  return (
    <View className="border-border bg-card gap-3 border-t px-5 py-4">
      {error ? (
        <Text className="text-destructive dark:text-foreground text-xs font-semibold">{error}</Text>
      ) : null}

      {!user ? (
        <>
          <Text className="text-muted-foreground text-center text-[10px] font-semibold uppercase tracking-widest">
            Niezalogowani użytkownicy nie mogą dołączyć do wydarzenia
          </Text>
          <Button className="w-full" onPress={() => setSheet("login")}>
            <Text>Zaloguj się, aby dołączyć</Text>
          </Button>
        </>
      ) : waitlisted ? (
        <>
          <Text className="text-muted-foreground text-center text-sm">
            Jesteś na liście rezerwowej. Dostaniesz miejsce, gdy ktoś zrezygnuje.
          </Text>
          <Button
            variant="outline"
            className="w-full"
            disabled={pending}
            onPress={() => setSheet("leave")}
          >
            <Text>Opuść listę rezerwową</Text>
          </Button>
        </>
      ) : event.registrationStatus ? (
        <>
          <Button className="w-full" onPress={onAddToCalendar}>
            <Text>Dodaj do kalendarza</Text>
          </Button>
          <Button
            variant="outline"
            className="w-full"
            disabled={pending}
            onPress={() => setSheet("leave")}
          >
            <Text>Opuść wydarzenie</Text>
          </Button>
        </>
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

      <BottomSheet visible={sheet === "calendar"} onClose={() => setSheet(null)}>
        <View className="gap-1">
          <Text variant="h3" className="text-2xl font-bold">
            Dodano Cię do wydarzenia
          </Text>
          <Text className="text-muted-foreground text-sm">
            Chcesz zapisać to wydarzenie w swoim kalendarzu?
          </Text>
        </View>
        <View className="mt-6 gap-3">
          <Button className="w-full" onPress={onAddToCalendar}>
            <Text>Dodaj do kalendarza</Text>
          </Button>
          <Button variant="outline" className="w-full" onPress={() => setSheet(null)}>
            <Text>Nie teraz</Text>
          </Button>
        </View>
      </BottomSheet>

      <BottomSheet visible={sheet === "login"} onClose={() => setSheet(null)}>
        <View className="gap-1">
          <Text variant="h3" className="text-2xl font-bold">
            Zaloguj się
          </Text>
          <Text className="text-muted-foreground text-sm">
            Po zalogowaniu wrócisz do tego wydarzenia.
          </Text>
        </View>
        <View className="mt-6 gap-3">
          <Button className="w-full" onPress={() => onLogin(KEYCLOAK_URL)}>
            <Text>Zaloguj się przez SSO</Text>
          </Button>
          <Button variant="outline" className="w-full" onPress={() => onLogin(DISCORD_URL)}>
            <DiscordIcon size={20} color={theme.foreground} />
            <Text>Kontynuuj przez Discord</Text>
          </Button>
        </View>
      </BottomSheet>

      <BottomSheet visible={sheet === "leave"} onClose={() => setSheet(null)}>
        <View className="gap-1">
          <Text variant="h3" className="text-2xl font-bold">
            {waitlisted ? "Opuścić listę rezerwową?" : "Opuścić wydarzenie?"}
          </Text>
          <Text className="text-muted-foreground text-sm">
            {waitlisted ? "Stracisz swoje miejsce w kolejce." : "Twoje miejsce zostanie zwolnione."}
          </Text>
        </View>
        <View className="mt-6 gap-3">
          <Button variant="destructive" className="w-full" disabled={pending} onPress={onLeave}>
            <Text>
              {pending
                ? "Opuszczanie..."
                : waitlisted
                  ? "Opuść listę rezerwową"
                  : "Opuść wydarzenie"}
            </Text>
          </Button>
          <Button variant="outline" className="w-full" onPress={() => setSheet(null)}>
            <Text>Anuluj</Text>
          </Button>
        </View>
      </BottomSheet>
    </View>
  );
}
