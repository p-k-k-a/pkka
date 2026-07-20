import { EventCard } from "@/components/events/event-card";
import { CtaCard } from "@/components/home/cta-card";
import { Text } from "@/components/ui/text";
import { useList as useEventsList } from "@pkka/api";
import { router } from "expo-router";
import { ActivityIndicator, FlatList, View } from "react-native";

const CTA = {
  title: "Chcesz brać udział w wydarzeniach zamkniętych?",
  subtitle:
    "Zaloguj się jako absolwent lub student Wydziału Informatyki AGH, aby uzyskać dostęp do pełnego kalendarza i mentoringu.",
  primaryLabel: "Zaloguj się do portalu",
};

function ListHeader() {
  return (
    <View className="px-5 pt-8 gap-8">
      <View>
        <View className="gap-1">
          <Text variant="h1" className="text-left text-4xl">
            Wydarzenia
          </Text>
        </View>
        <Text className="leading-6 text-muted-foreground">
          Przeglądaj publiczne spotkania i prelekcje organizowane przez społeczność Alumni WI AGH.
          Dołącz do nas i buduj sieć kontaktów.
        </Text>
      </View>
      <CtaCard {...CTA} onPrimary={() => router.push("/login")} />
    </View>
  );
}

export default function EventsScreen() {
  const { data, isLoading, isError } = useEventsList({ page: 0, size: 20 });

  const events = data?.data?.content ?? [];

  return (
    <FlatList
      className="flex-1 bg-background"
      data={events}
      keyExtractor={(item) => item.id!}
      renderItem={({ item }) => (
        <View className="px-5">
          <EventCard event={item} />
        </View>
      )}
      ItemSeparatorComponent={() => <View className="h-5" />}
      ListHeaderComponent={ListHeader}
      ListHeaderComponentStyle={{ marginBottom: 20 }}
      ListFooterComponent={
        <View className="px-5 pb-12 pt-8">
          {isLoading ? (
            <ActivityIndicator className="pb-8" />
          ) : isError ? (
            <Text className="pb-8 text-center text-sm text-muted-foreground">
              Nie udało się załadować wydarzeń.
            </Text>
          ) : null}
        </View>
      }
    />
  );
}
