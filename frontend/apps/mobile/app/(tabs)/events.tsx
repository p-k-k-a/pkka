import { EventCard } from "@/components/events/event-card";
import { CtaCard } from "@/components/home/cta-card";
import { PageHeader } from "@/components/ui/page-header";
import { Text } from "@/components/ui/text";
import { useListEvents } from "@pkka/api";
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
    <View className="gap-10">
      <PageHeader
        eyebrow="Kalendarz wydarzeń"
        title="Wydarzenia"
        description="Przeglądaj publiczne spotkania i prelekcje organizowane przez społeczność Alumni WI AGH. Dołącz do nas i buduj sieć kontaktów."
      />
      <View className="px-5">
        <CtaCard {...CTA} onPrimary={() => router.push("/login")} />
      </View>
    </View>
  );
}

export default function EventsScreen() {
  const { data, isLoading, isError } = useListEvents({ page: 0, size: 20 });

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
      ItemSeparatorComponent={() => <View className="h-6" />}
      ListHeaderComponent={ListHeader}
      ListHeaderComponentStyle={{ marginBottom: 24 }}
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
