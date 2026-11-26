import { GroupList } from "@/components/topic-groups/group-list";
import { RequestCard } from "@/components/topic-groups/request-card";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import {
  TopicGroupRequestStatus,
  useListMyTopicGroupRequests,
  useListTopicGroups,
} from "@pkka/api";
import { useThemeColors } from "@/lib/theme";
import { router } from "expo-router";
import { Plus } from "lucide-react-native";
import { useState } from "react";
import { ActivityIndicator, RefreshControl, ScrollView, View } from "react-native";

export function TopicGroups() {
  const theme = useThemeColors();
  const { data, isLoading, refetch } = useListMyTopicGroupRequests();
  const groups = useListTopicGroups({ size: 100 });
  const [refreshing, setRefreshing] = useState(false);
  const latest = data?.data[0];
  const request = latest?.status === TopicGroupRequestStatus.APPROVED ? undefined : latest;

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
        {request?.status === TopicGroupRequestStatus.UNDER_REVIEW ? null : (
          <Button className="mt-2" onPress={() => router.push("/topic-groups/request")}>
            <Plus size={18} color={theme.primaryForeground} />
            <Text className="font-semibold">Stwórz nową grupę</Text>
          </Button>
        )}
      </View>

      {isLoading || groups.isLoading ? (
        <View className="py-16">
          <ActivityIndicator />
        </View>
      ) : (
        <>
          {request ? <RequestCard request={request} /> : null}
          <GroupList groups={groups.data?.data.content} isError={groups.isError} />
        </>
      )}
    </ScrollView>
  );
}
