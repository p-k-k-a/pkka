import { DiscordBanner } from "@/components/topic-groups/discord-banner";
import { MembershipButton } from "@/components/topic-groups/membership-button";
import { InitialsAvatar } from "@/components/ui/initials-avatar";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import { cn } from "@/lib/utils";
import {
  DiscordStatus,
  useGetMyDiscordStatus,
  useGetMyProfile,
  useListTopicGroupMembers,
  useMe,
  type TopicGroupMemberResponse,
  type TopicGroupResponse,
} from "@pkka/api";
import {
  avatarInitials,
  isAdmin,
  topicGroupMemberCountLabel,
  topicGroupMemberName,
  topicGroupMemberShortName,
} from "@pkka/domain";
import { router } from "expo-router";
import { ArrowUpRight, ChevronDown, ChevronUp, MessageCircle, Users, X } from "lucide-react-native";
import { useState } from "react";
import { Linking, Modal, Pressable, ScrollView, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

const STACKED_AVATARS = 3;

function GroupAvatar({ name }: { name: string }) {
  return (
    <View className="bg-primary/15 size-12 items-center justify-center rounded-xl">
      <Text className="text-base font-bold text-amber-800 dark:text-primary">
        {avatarInitials(name)}
      </Text>
    </View>
  );
}

function CoordinatorBadge() {
  return (
    <View className="bg-primary/15 rounded-md px-2 py-0.5">
      <Text className="text-xs font-bold text-amber-800 dark:text-primary">Koordynator</Text>
    </View>
  );
}

type MembersProps = {
  members: TopicGroupMemberResponse[];
  memberCount: number;
  myId: string | undefined;
  onOpenProfile: (id: string) => void;
};

function Members({ members, memberCount, myId, onOpenProfile }: MembersProps) {
  const theme = useThemeColors();
  const [expanded, setExpanded] = useState(false);
  const displayName = (member: TopicGroupMemberResponse) =>
    member.id === myId ? "Ty" : topicGroupMemberShortName(member);
  const hidden = members.length - STACKED_AVATARS;

  return (
    <View className="gap-3">
      <Pressable
        role="button"
        onPress={() => setExpanded((open) => !open)}
        className="flex-row items-center gap-3 active:opacity-70"
      >
        <View className="flex-row items-center">
          {members.slice(0, STACKED_AVATARS).map((member, index) => (
            <InitialsAvatar
              key={member.id}
              name={topicGroupMemberName(member)}
              size="xs"
              className={cn("border-card border-2", index > 0 && "-ml-2")}
            />
          ))}
          {hidden > 0 ? (
            <Text className="text-muted-foreground ml-1.5 text-xs font-semibold">+{hidden}</Text>
          ) : null}
        </View>
        <Text className="text-foreground flex-1 text-base font-semibold">
          {topicGroupMemberCountLabel(memberCount)}
        </Text>
        <View className="flex-row items-center gap-1">
          <Text className="text-accent text-sm font-semibold">{expanded ? "Zwiń" : "Rozwiń"}</Text>
          {expanded ? (
            <ChevronUp size={16} color={theme.accent} />
          ) : (
            <ChevronDown size={16} color={theme.accent} />
          )}
        </View>
      </Pressable>

      {expanded ? (
        <View>
          {members.map((member) => (
            <Pressable
              key={member.id}
              role="link"
              onPress={() => onOpenProfile(member.id)}
              className="flex-row items-center gap-3 py-1.5 active:opacity-70"
            >
              <InitialsAvatar name={topicGroupMemberName(member)} size="sm" />
              <Text className="text-foreground flex-1 text-base">{displayName(member)}</Text>
              {member.coordinator ? <CoordinatorBadge /> : null}
            </Pressable>
          ))}
        </View>
      ) : null}
    </View>
  );
}

function DiscordChannelCard({ url }: { url: string }) {
  const theme = useThemeColors();
  return (
    <Pressable
      role="link"
      onPress={() => void Linking.openURL(url)}
      className="bg-band flex-row items-center gap-3 rounded-2xl p-3 active:opacity-80"
    >
      <View className="size-11 items-center justify-center rounded-xl bg-white/10">
        <MessageCircle size={20} color={theme.bandForeground} />
      </View>
      <View className="flex-1 gap-0.5">
        <Text className="text-band-foreground text-base font-semibold">Kanał na Discordzie</Text>
        <Text className="text-band-foreground/70 text-sm">Otwórz kanał grupy</Text>
      </View>
      <View className="bg-primary size-12 items-center justify-center rounded-full">
        <ArrowUpRight size={20} color={theme.primaryForeground} />
      </View>
    </Pressable>
  );
}

type GroupSheetProps = {
  group: TopicGroupResponse | null;
  onClose: () => void;
};

export function GroupSheet({ group, onClose }: GroupSheetProps) {
  const theme = useThemeColors();
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { data: me } = useMe({ query: { enabled: !!user } });
  const canSeeMembers = !!group && (group.member || isAdmin(me?.data.roles));
  const { data: members } = useListTopicGroupMembers(group?.id ?? "", {
    query: { enabled: canSeeMembers },
  });
  const { data: profile } = useGetMyProfile({ query: { enabled: canSeeMembers } });
  const { data: discord } = useGetMyDiscordStatus({ query: { enabled: !!group?.member } });

  if (!group) return null;

  const memberList = members?.status === 200 ? members.data : undefined;
  const myId = profile?.status === 200 ? profile.data.id : undefined;
  const coordinator = memberList?.find((member) => member.coordinator);
  const coordinatorLabel = group.coordinator
    ? "Ty"
    : coordinator
      ? topicGroupMemberShortName(coordinator)
      : undefined;
  const onServer = discord?.data.status === DiscordStatus.ON_SERVER;

  const openProfile = (id: string) => {
    onClose();
    router.push({ pathname: "/alumni/[id]", params: { id } });
  };

  return (
    <Modal
      visible
      transparent
      animationType="none"
      statusBarTranslucent
      navigationBarTranslucent
      onRequestClose={onClose}
    >
      <View
        className="flex-1 justify-center bg-black/40 px-4"
        style={{ paddingTop: insets.top + 16, paddingBottom: insets.bottom + 16 }}
      >
        <Pressable accessibilityLabel="Zamknij" onPress={onClose} className="absolute inset-0" />
        <View className="bg-card max-h-full overflow-hidden rounded-3xl">
          <ScrollView bounces={false} contentContainerStyle={{ padding: 20, gap: 16 }}>
            <View className="flex-row items-center gap-3">
              <GroupAvatar name={group.name} />
              <View className="flex-1 gap-0.5">
                <Text
                  role="heading"
                  className="font-heading text-foreground text-xl font-semibold leading-tight"
                >
                  {group.name}
                </Text>
                {coordinatorLabel ? (
                  <Text className="text-muted-foreground text-sm">
                    Koordynator: {coordinatorLabel}
                  </Text>
                ) : null}
              </View>
              <Pressable
                role="button"
                accessibilityLabel="Zamknij"
                onPress={onClose}
                hitSlop={8}
                className="bg-muted dark:bg-background size-11 items-center justify-center rounded-full active:opacity-70"
              >
                <X size={20} color={theme.foreground} />
              </Pressable>
            </View>

            {group.description ? (
              <Text className="text-muted-foreground text-base leading-relaxed">
                {group.description}
              </Text>
            ) : null}

            <View className="border-border border-t pt-4">
              {canSeeMembers && memberList ? (
                <Members
                  members={memberList}
                  memberCount={group.memberCount}
                  myId={myId}
                  onOpenProfile={openProfile}
                />
              ) : (
                <View className="flex-row items-center gap-2">
                  <Users size={18} color={theme.mutedForeground} />
                  <Text className="text-foreground flex-1 text-base font-semibold">
                    {topicGroupMemberCountLabel(group.memberCount)}
                  </Text>
                  {!group.member ? (
                    <Text className="text-muted-foreground text-xs">Widoczni po dołączeniu</Text>
                  ) : null}
                </View>
              )}
            </View>

            {group.member ? (
              <View className="border-border gap-3 border-t pt-4">
                {onServer && group.discordChannelUrl ? (
                  <DiscordChannelCard url={group.discordChannelUrl} />
                ) : (
                  <DiscordBanner />
                )}
                {group.coordinator ? (
                  <Text className="text-muted-foreground text-[13px] leading-relaxed">
                    Aby odejść, przekaż grupę innemu członkowi na stronie klubu.
                  </Text>
                ) : (
                  <MembershipButton group={group} className="w-full" />
                )}
              </View>
            ) : (
              <MembershipButton group={group} prominent className="w-full" />
            )}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}
