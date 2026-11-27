import { GroupList } from "@/components/topic-groups/group-list";
import { TopicGroupRequestCard } from "@/components/topic-groups/request-card";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import {
  TopicGroupRequestStatus,
  useListMyTopicGroupRequests,
  useListTopicGroups,
} from "@pkka/api";
import { useThemeColors } from "@/lib/theme";
import { Link } from "expo-router";
import { Plus } from "lucide-react-native";
import { useState } from "react";
import { ActivityIndicator, RefreshControl, ScrollView, View } from "react-native";

export function TopicGroups() {
  const theme = useThemeColors();
  const requestsQuery = useListMyTopicGroupRequests();
  const groupsQuery = useListTopicGroups({ size: 100 });
  const [refreshing, setRefreshing] = useState(false);
  const latestRequest = requestsQuery.data?.data[0];
  const openRequest =
    latestRequest?.status === TopicGroupRequestStatus.APPROVED ? undefined : latestRequest;

  const onRefresh = async () => {
    setRefreshing(true);
    await Promise.all([requestsQuery.refetch(), groupsQuery.refetch()]);
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
        {openRequest?.status === TopicGroupRequestStatus.UNDER_REVIEW ? null : (
          <Link href="/topic-groups/request" asChild>
            <Button className="mt-2">
              <Plus size={18} color={theme.primaryForeground} />
              <Text className="font-semibold">Stwórz nową grupę</Text>
            </Button>
          </Link>
        )}
      </View>

      {requestsQuery.isLoading || groupsQuery.isLoading ? (
        <View className="py-16">
          <ActivityIndicator />
        </View>
      ) : (
        <>
          {openRequest ? <TopicGroupRequestCard request={openRequest} /> : null}
          <GroupList groups={groupsQuery.data?.data.content} isError={groupsQuery.isError} />
        </>
      )}
    </ScrollView>
  );
}
