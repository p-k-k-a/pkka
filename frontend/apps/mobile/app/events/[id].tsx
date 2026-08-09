import { EventDetailView } from "@/components/events/event-detail-view";
import { Button } from "@/components/ui/button";
import { DetailHeader } from "@/components/ui/detail-header";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useGetEventById } from "@pkka/api";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, View } from "react-native";

export default function EventDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { user } = useAuth();
  const { data, isError, isPending } = useGetEventById(id ?? "", {
    query: { enabled: !!id },
  });

  const event = data?.data;

  return (
    <View className="flex-1 bg-background">
      <DetailHeader title="Szczegóły wydarzenia" onBack={() => router.back()} />

      {isError ? (
        <Text className="py-12 text-center text-sm text-muted-foreground">
          Nie udało się załadować wydarzenia.
        </Text>
      ) : isPending ? (
        <ActivityIndicator className="py-12" />
      ) : event ? (
        <>
          <EventDetailView event={event} />
          <View className="gap-3 border-t border-border bg-background px-5 py-4">
            {user ? (
              <Button className="w-full">
                <Text>Dołącz do wydarzenia</Text>
              </Button>
            ) : (
              <>
                <Text className="text-center text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
                  Niezalogowani użytkownicy nie mogą dołączyć do wydarzenia
                </Text>
                <Button className="w-full" onPress={() => router.push("/login")}>
                  <Text>Zaloguj się, aby dołączyć</Text>
                </Button>
              </>
            )}
          </View>
        </>
      ) : null}
    </View>
  );
}
