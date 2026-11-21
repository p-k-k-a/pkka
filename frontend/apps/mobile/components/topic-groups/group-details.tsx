import { useIsAlumni } from "@/components/auth/require-alumni";
import { DiscordBanner } from "@/components/topic-groups/discord-banner";
import { MembershipButton } from "@/components/topic-groups/membership-button";
import { InitialsAvatar } from "@/components/ui/initials-avatar";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import {
  useGetTopicGroup,
  useListTopicGroupMembers,
  useMe,
  type TopicGroupMemberResponse,
} from "@pkka/api";
import { isAdmin, topicGroupMemberCountLabel, topicGroupMemberName } from "@pkka/domain";
import { router } from "expo-router";
import { ChevronRight, Users } from "lucide-react-native";
import { useState } from "react";
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, View } from "react-native";

function MemberRow({ member }: { member: TopicGroupMemberResponse }) {
  const theme = useThemeColors();
  const name = topicGroupMemberName(member);

  return (
    <Pressable
      role="link"
      onPress={() => router.push({ pathname: "/alumni/[id]", params: { id: member.id } })}
      className="flex-row items-center gap-3 py-2.5 active:opacity-70"
    >
      <InitialsAvatar name={name} size="sm" />
      <View className="flex-1 gap-0.5">
        <Text className="text-foreground text-base font-semibold">{name}</Text>
        {member.coordinator ? (
          <Text className="text-muted-foreground text-[13px]">Koordynator</Text>
        ) : null}
      </View>
      <ChevronRight size={20} color={theme.mutedForeground} />
    </Pressable>
  );
}

function Members({ groupId }: { groupId: string }) {
  const { data, isLoading, isError } = useListTopicGroupMembers(groupId);

  return (
    <View className="gap-2">
      <Text
        role="heading"
        aria-level="2"
        className="font-heading text-foreground text-[23px] font-semibold"
      >
        Członkowie
      </Text>
      {isLoading ? (
        <ActivityIndicator />
      ) : isError ? (
        <Text className="text-muted-foreground text-sm">Nie udało się załadować członków.</Text>
      ) : (
        <View>
          {data?.status === 200
            ? data.data.map((member) => <MemberRow key={member.id} member={member} />)
            : null}
        </View>
      )}
    </View>
  );
}

export function TopicGroupDetails({ slug }: { slug: string }) {
  const theme = useThemeColors();
  const { user } = useAuth();
  const isAlumni = useIsAlumni();
  const { data: me } = useMe({ query: { enabled: !!user } });
  const { data, isLoading, isError, refetch } = useGetTopicGroup(slug, {
    query: { enabled: isAlumni },
  });
  const [refreshing, setRefreshing] = useState(false);
  const group = data?.data;
  const canSeeMembers = !!group && (group.member || isAdmin(me?.data.roles));

  const onRefresh = async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  };

  if (isLoading) {
    return (
      <View className="py-16">
        <ActivityIndicator />
      </View>
    );
  }

  if (isError || !group) {
    return (
      <Text className="text-muted-foreground px-5 py-16 text-center text-sm">
        Nie udało się załadować grupy.
      </Text>
    );
  }

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
          {group.name}
        </Text>
        <View className="flex-row items-center gap-1.5">
          <Users size={16} color={theme.mutedForeground} />
          <Text className="text-muted-foreground text-sm font-medium">
            {topicGroupMemberCountLabel(group.memberCount)}
          </Text>
        </View>
        {group.description ? (
          <Text className="text-muted-foreground text-sm leading-relaxed">{group.description}</Text>
        ) : null}
      </View>

      {group.coordinator ? (
        <View className="bg-muted dark:bg-card gap-1 rounded-2xl p-5">
          <Text className="text-foreground text-base font-semibold">Koordynujesz tę grupę</Text>
          <Text className="text-muted-foreground text-sm leading-relaxed">
            Aby odejść, przekaż grupę innemu członkowi na stronie klubu.
          </Text>
        </View>
      ) : (
        <MembershipButton group={group} prominent className="w-full" />
      )}

      {group.member ? <DiscordBanner /> : null}

      {canSeeMembers ? (
        <Members groupId={group.id} />
      ) : (
        <Text className="text-muted-foreground text-sm leading-relaxed">
          Dołącz do grupy, żeby zobaczyć jej członków.
        </Text>
      )}
    </ScrollView>
  );
}
