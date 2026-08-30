import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { addEventToCalendar } from "@/lib/calendar";
import {
  ApiError,
  getGetEventByIdQueryKey,
  getListEventsQueryKey,
  useRegister,
  useUnregister,
  type EventDetailsResponse,
} from "@pkka/api";
import { useQueryClient } from "@tanstack/react-query";
import { router } from "expo-router";
import { useState } from "react";
import { View } from "react-native";

type ActiveSheet = "calendar" | "leave" | null;
// In ideal world we would have this enum from the backend from openapi but I don't want t odo that myself and I don't want to wait for backend to be updated so let's stick with it
type ConflictReason =
  | "ALREADY_REGISTERED"
  | "REGISTRATION_CLOSED"
  | "NO_SEATS_LEFT"
  | "EVENT_ALREADY_STARTED";

const CONFLICT_MESSAGES: Record<ConflictReason, string> = {
  ALREADY_REGISTERED: "Jesteś już zapisany na to wydarzenie.",
  REGISTRATION_CLOSED: "Rejestracja na to wydarzenie została zamknięta.",
  NO_SEATS_LEFT: "Brak wolnych miejsc.",
  EVENT_ALREADY_STARTED: "Wydarzenie już się rozpoczęło.",
};

function errorMessage(error: unknown, action: "join" | "leave"): string {
  if (error instanceof ApiError) {
    const reason = (error.body as { reason?: string } | null)?.reason;
    if (reason && CONFLICT_MESSAGES[reason as ConflictReason])
      return CONFLICT_MESSAGES[reason as ConflictReason];
    if (error.status === 404 && action === "leave") return "Nie jesteś zapisany na to wydarzenie.";
    if (error.status === 404) return "Nie znaleziono wydarzenia.";
  }
  return action === "join"
    ? "Nie udało się zapisać na wydarzenie. Spróbuj ponownie."
    : "Nie udało się opuścić wydarzenia. Spróbuj ponownie.";
}

export function EventParticipationFooter({ event }: { event: EventDetailsResponse }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [sheet, setSheet] = useState<ActiveSheet>(null);
  const [error, setError] = useState<string | null>(null);

  const register = useRegister();
  const unregister = useUnregister();
  const pending = register.isPending || unregister.isPending;

  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: getGetEventByIdQueryKey(event.id) }),
      queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() }),
    ]);

  const onJoin = async () => {
    setError(null);
    try {
      await register.mutateAsync({ eventId: event.id });
      await refresh();
      setSheet("calendar");
    } catch (e) {
      setError(errorMessage(e, "join"));
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
      setError(errorMessage(e, "leave"));
      await refresh();
    }
  };

  const onAddToCalendar = async () => {
    setSheet(null);
    try {
      await addEventToCalendar(event);
    } catch {
      setError("Nie udało się otworzyć kalendarza.");
    }
  };

  return (
    <View className="border-border bg-background gap-3 border-t px-5 py-4">
      {error ? <Text className="text-destructive text-xs font-semibold">{error}</Text> : null}

      {!user ? (
        <>
          <Text className="text-muted-foreground text-center text-[10px] font-semibold uppercase tracking-widest">
            Niezalogowani użytkownicy nie mogą dołączyć do wydarzenia
          </Text>
          <Button className="w-full" onPress={() => router.push("/login")}>
            <Text>Zaloguj się, aby dołączyć</Text>
          </Button>
        </>
      ) : event.registered ? (
        <>
          <Button className="w-full" onPress={onAddToCalendar}>
            <Text>Dodaj do kalendarza</Text>
          </Button>
          <Button
            variant="outline"
            className="w-full active:bg-muted"
            disabled={pending}
            onPress={() => setSheet("leave")}
          >
            <Text className="group-active:text-foreground">Opuść wydarzenie</Text>
          </Button>
        </>
      ) : (
        <Button className="w-full" disabled={pending} onPress={onJoin}>
          <Text>{pending ? "Zapisywanie..." : "Dołącz do wydarzenia"}</Text>
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
          <Button
            variant="outline"
            className="w-full active:bg-muted"
            onPress={() => setSheet(null)}
          >
            <Text className="group-active:text-foreground">Nie teraz</Text>
          </Button>
        </View>
      </BottomSheet>

      <BottomSheet visible={sheet === "leave"} onClose={() => setSheet(null)}>
        <View className="gap-1">
          <Text variant="h3" className="text-2xl font-bold">
            Opuścić wydarzenie?
          </Text>
          <Text className="text-muted-foreground text-sm">Twoje miejsce zostanie zwolnione.</Text>
        </View>
        <View className="mt-6 gap-3">
          <Button variant="destructive" className="w-full" disabled={pending} onPress={onLeave}>
            <Text>{pending ? "Opuszczanie..." : "Opuść wydarzenie"}</Text>
          </Button>
          <Button
            variant="outline"
            className="w-full active:bg-muted"
            onPress={() => setSheet(null)}
          >
            <Text className="group-active:text-foreground">Anuluj</Text>
          </Button>
        </View>
      </BottomSheet>
    </View>
  );
}
