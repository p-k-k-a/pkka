import { RequestCard } from "@/components/topic-groups/request-card";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { TopicGroupRequestStatus, useListMyTopicGroupRequests } from "@pkka/api";
import { router } from "expo-router";
import { ChevronRight, Plus } from "lucide-react-native";
import { useState } from "react";
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, View } from "react-native";

function openRequestForm() {
  router.push("/topic-groups/request");
}

function ProposeGroupRow() {
  const theme = useThemeColors();
  return (
    <Pressable
      role="button"
      onPress={openRequestForm}
      className="bg-muted flex-row items-center gap-3 rounded-xl px-4 py-4 active:opacity-70"
    >
      <View className="bg-primary h-10 w-10 items-center justify-center rounded-full">
        <Plus size={20} color={theme.primaryForeground} />
      </View>
      <View className="flex-1 gap-0.5">
        <Text className="text-foreground text-base font-semibold">Brakuje grupy dla Ciebie?</Text>
        <Text className="text-muted-foreground text-sm">Zaproponuj własną</Text>
      </View>
      <ChevronRight size={20} color={theme.foreground} />
    </Pressable>
  );
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
          Dołącz do alumnów o podobnych zainteresowaniach.
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
      ) : (
        <View className="gap-4">
          {latest?.status === TopicGroupRequestStatus.UNDER_REVIEW ? null : <ProposeGroupRow />}
          {latest ? <RequestCard request={latest} /> : null}
        </View>
      )}
    </ScrollView>
  );
}
