import { CtaCard } from "@/components/home/cta-card";
import { RequestCard } from "@/components/topic-groups/request-card";
import { Text } from "@/components/ui/text";
import { useListMyTopicGroupRequests } from "@pkka/api";
import { router } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, RefreshControl, ScrollView, View } from "react-native";

function openRequestForm() {
  router.push("/topic-groups/request");
}

export function TopicGroups() {
  const { data, isLoading, isError, refetch } = useListMyTopicGroupRequests();
  const [refreshing, setRefreshing] = useState(false);
  const latest = data?.data[0];

  const onRefresh = async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  };

  return (
    <ScrollView
      className="flex-1"
      showsVerticalScrollIndicator={false}
      contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 24, paddingBottom: 48, gap: 24 }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <View className="gap-3">
        <Text
          role="heading"
          aria-level="1"
          className="font-heading text-foreground text-[33px] font-semibold leading-tight tracking-tight"
        >
          Grupy tematyczne
        </Text>
        <Text className="text-muted-foreground text-sm leading-relaxed">
          Miejsce dla alumnów o podobnych zainteresowaniach. Jeśli brakuje grupy dla Ciebie,
          zaproponuj własną.
        </Text>
      </View>

      {isLoading ? (
        <View className="py-16">
          <ActivityIndicator />
        </View>
      ) : isError && !latest ? (
        <Text className="text-muted-foreground py-16 text-center text-sm">
          Nie udało się załadować Twojego wniosku.
        </Text>
      ) : latest ? (
        <RequestCard request={latest} onPropose={openRequestForm} />
      ) : (
        <CtaCard
          title="Brakuje grupy dla Ciebie?"
          subtitle="Zaproponuj nową. Administracja rozpatrzy wniosek, a po akceptacji zostaniesz właścicielem grupy."
          primaryLabel="Zaproponuj grupę"
          onPrimary={openRequestForm}
        />
      )}
    </ScrollView>
  );
}
