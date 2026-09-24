import { EventDetailView } from "@/components/events/event-detail-view";
import { Button } from "@/components/ui/button";
import { DetailHeader } from "@/components/ui/detail-header";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { useGetEventById } from "@pkka/api";
import { ArrowRight } from "lucide-react-native";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, View } from "react-native";

export default function EventDetailScreen() {
  const theme = useThemeColors();
  const { id } = useLocalSearchParams<{ id: string }>();
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
          <View className="gap-3 border-t border-border bg-card px-5 py-4">
            <Text className="text-center text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
              Niezalogowani użytkownicy nie mogą dołączyć do wydarzenia
            </Text>
            <Button size="lg" className="w-full" onPress={() => router.push("/login")}>
              <Text className="text-base font-semibold">Zaloguj się, aby dołączyć</Text>
              <ArrowRight size={18} color={theme.primaryForeground} />
            </Button>
          </View>
        </>
      ) : null}
    </View>
  );
}
