import { EventDetailView } from "@/components/events/event-detail-view";
import { EventParticipationFooter } from "@/components/events/event-participation-footer";
import { DetailHeader } from "@/components/ui/detail-header";
import { Text } from "@/components/ui/text";
import { useGetEventBySlug } from "@pkka/api";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, View } from "react-native";

export default function EventDetailScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const { data, isError, isPending } = useGetEventBySlug(slug ?? "", {
    query: { enabled: !!slug },
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
          <EventParticipationFooter event={event} />
        </>
      ) : null}
    </View>
  );
}
