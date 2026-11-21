import { MembershipButton } from "@/components/topic-groups/membership-button";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { type TopicGroupResponse } from "@pkka/api";
import { topicGroupMemberCountLabel } from "@pkka/domain";
import { router } from "expo-router";
import { ChevronRight, Users } from "lucide-react-native";
import { Pressable, View } from "react-native";

function GroupRow({ group }: { group: TopicGroupResponse }) {
  const theme = useThemeColors();

  return (
    <View className="border-muted-foreground/25 dark:border-border bg-card gap-3 rounded-2xl border p-5">
      <Pressable
        role="link"
        onPress={() =>
          router.push({ pathname: "/topic-groups/[slug]", params: { slug: group.slug } })
        }
        className="gap-2 active:opacity-70"
      >
        <View className="flex-row items-center gap-2">
          <Text className="font-heading text-foreground flex-1 text-lg font-semibold leading-snug">
            {group.name}
          </Text>
          <ChevronRight size={20} color={theme.mutedForeground} />
        </View>
        {group.description ? (
          <Text numberOfLines={2} className="text-muted-foreground text-sm leading-relaxed">
            {group.description}
          </Text>
        ) : null}
      </Pressable>
      <View className="flex-row items-center gap-3">
        <View className="flex-1 flex-row items-center gap-1.5">
          <Users size={16} color={theme.mutedForeground} />
          <Text className="text-muted-foreground text-[13px] font-medium">
            {topicGroupMemberCountLabel(group.memberCount)}
          </Text>
        </View>
        {group.coordinator ? (
          <Text className="text-muted-foreground text-[13px] font-semibold">Koordynujesz</Text>
        ) : (
          <MembershipButton group={group} />
        )}
      </View>
    </View>
  );
}

type GroupListProps = {
  groups: TopicGroupResponse[] | undefined;
  isLoading: boolean;
  isError: boolean;
};

export function GroupList({ groups, isLoading, isError }: GroupListProps) {
  return (
    <View className="gap-3">
      <Text
        role="heading"
        aria-level="2"
        className="font-heading text-foreground text-[23px] font-semibold"
      >
        Wszystkie grupy
      </Text>
      {isLoading ? null : isError && !groups ? (
        <Text className="text-muted-foreground py-8 text-center text-sm">
          Nie udało się załadować grup.
        </Text>
      ) : !groups?.length ? (
        <Text className="text-muted-foreground py-8 text-center text-sm">
          Nie ma jeszcze żadnej grupy. Zaproponuj pierwszą.
        </Text>
      ) : (
        groups.map((group) => <GroupRow key={group.id} group={group} />)
      )}
    </View>
  );
}
