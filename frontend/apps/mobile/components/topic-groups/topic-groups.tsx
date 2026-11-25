import { GroupList } from "@/components/topic-groups/group-list";
import { RequestCard } from "@/components/topic-groups/request-card";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import {
  TopicGroupRequestStatus,
  useListMyTopicGroupRequests,
  useListTopicGroups,
} from "@pkka/api";
import { router } from "expo-router";
import { Plus } from "lucide-react-native";
import { useState } from "react";
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, View } from "react-native";

function openRequestForm() {
  router.push("/topic-groups/request");
}

function ProposeGroupCard() {
  const theme = useThemeColors();
  return (
    <Pressable
      role="button"
      onPress={openRequestForm}
      className="border-muted-foreground/25 dark:border-border flex-row items-center gap-3 rounded-[20px] border-[1.5px] border-dashed p-3.5 active:opacity-70"
    >
      <View className="border-primary size-11 items-center justify-center rounded-xl border-[1.5px] border-dashed">
        <Plus size={20} color={theme.primary} />
      </View>
      <View className="flex-1 gap-0.5">
        <Text className="text-foreground text-[15px] font-semibold">Brakuje grupy dla Ciebie?</Text>
        <Text className="text-muted-foreground text-[13px]">Zaproponuj własną</Text>
      </View>
    </Pressable>
  );
}

export function TopicGroups() {
  const { data, isLoading, refetch } = useListMyTopicGroupRequests();
  const groups = useListTopicGroups({ size: 100 });
  const [refreshing, setRefreshing] = useState(false);
  const latest = data?.data[0];

  const onRefresh = async () => {
    setRefreshing(true);
    await Promise.all([refetch(), groups.refetch()]);
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
          Dołącz do dyskusji na interesujące Cię tematy z innymi alumnami!
        </Text>
      </View>

      {isLoading || groups.isLoading ? (
        <View className="py-16">
          <ActivityIndicator />
        </View>
      ) : (
        <>
          <GroupList groups={groups.data?.data.content} isError={groups.isError} />
          {latest ? <RequestCard request={latest} /> : null}
          {latest?.status === TopicGroupRequestStatus.UNDER_REVIEW ? null : <ProposeGroupCard />}
        </>
      )}
    </ScrollView>
  );
}
