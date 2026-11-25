import { GroupList } from "@/components/topic-groups/group-list";
import { RequestCard } from "@/components/topic-groups/request-card";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import {
  TopicGroupRequestStatus,
  useListMyTopicGroupRequests,
  useListTopicGroups,
} from "@pkka/api";
import { router } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, RefreshControl, ScrollView, View } from "react-native";

function ProposeGroupCard() {
  return (
    <View className="bg-band dark:border-border dark:bg-card gap-1.5 rounded-2xl p-5 dark:border">
      <Text
        role="heading"
        aria-level="2"
        className="font-heading text-band-foreground text-xl font-semibold leading-tight"
      >
        Brakuje grupy dla Ciebie?
      </Text>
      <Text className="text-band-foreground/80 text-sm leading-relaxed">
        Zaproponuj własną. Po akceptacji zostaniesz jej koordynatorem.
      </Text>
      <Button className="mt-3 w-full" onPress={() => router.push("/topic-groups/request")}>
        <Text className="font-semibold">Zaproponuj grupę</Text>
      </Button>
    </View>
  );
}

export function TopicGroups() {
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
      </View>

      {isLoading || groups.isLoading ? (
        <View className="py-16">
          <ActivityIndicator />
        </View>
      ) : (
        <>
          <GroupList groups={groups.data?.data.content} isError={groups.isError} />
          {request ? <RequestCard request={request} /> : null}
          {request?.status === TopicGroupRequestStatus.UNDER_REVIEW ? null : <ProposeGroupCard />}
        </>
      )}
    </ScrollView>
  );
}
