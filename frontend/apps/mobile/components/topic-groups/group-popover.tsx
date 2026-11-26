import { useIsAlumni } from "@/components/auth/require-alumni";
import { DiscordBanner } from "@/components/topic-groups/discord-banner";
import { CoordinatorPill, Pill } from "@/components/topic-groups/group-visuals";
import { useToggleMembership } from "@/components/topic-groups/use-toggle-membership";
import { Button } from "@/components/ui/button";
import { InitialsAvatar } from "@/components/ui/initials-avatar";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import {
  DiscordStatus,
  useGetMyDiscordStatus,
  useGetMyProfile,
  useListTopicGroupMembers,
  useMe,
  type TopicGroupResponse,
} from "@pkka/api";
import {
  isAdmin,
  topicGroupMemberCountLabel,
  topicGroupMemberName,
  topicGroupMemberShortName,
} from "@pkka/domain";
import { DiscordIcon } from "@pkka/icons/native";
import { router } from "expo-router";
import { ChevronDown, ChevronRight, LogOut, Users, X } from "lucide-react-native";
import { useState } from "react";
import {
  Linking,
  Modal,
  Pressable,
  ScrollView,
  View,
  useWindowDimensions,
  type LayoutChangeEvent,
} from "react-native";
import Animated, { FadeIn } from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export type CardRect = { x: number; y: number; width: number; height: number };

const TAB_BAR_HEIGHT = 76;

type GroupPopoverProps = {
  group: TopicGroupResponse;
  rect: CardRect;
  onClose: () => void;
};

export function GroupPopover({ group, rect, onClose }: GroupPopoverProps) {
  const theme = useThemeColors();
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const { user } = useAuth();
  const isAlumni = useIsAlumni();
  const { data: me } = useMe({ query: { enabled: !!user } });
  const canSeeMembers = group.member || isAdmin(me?.data.roles);
  const { data: members } = useListTopicGroupMembers(group.id, {
    query: { enabled: isAlumni && canSeeMembers },
  });
  const { data: profile } = useGetMyProfile({ query: { enabled: canSeeMembers } });
  const { data: discord } = useGetMyDiscordStatus({ query: { enabled: group.member } });
  const { toggle, isPending } = useToggleMembership(group);
  const [expanded, setExpanded] = useState(false);
  const [top, setTop] = useState<number | null>(null);

  const maxBottom = windowHeight - insets.bottom - TAB_BAR_HEIGHT - 12;
  const cardTop = rect.y - 4;
  const maxHeight = maxBottom - (top ?? cardTop);

  const onContentLayout = (event: LayoutChangeEvent) => {
    const contentHeight = event.nativeEvent.layout.height;
    setTop(Math.max(insets.top + 12, Math.min(cardTop, maxBottom - contentHeight)));
  };

  const memberList = members?.status === 200 ? members.data : undefined;
  const myId = profile?.status === 200 ? profile.data.id : undefined;
  const coordinator = memberList?.find((member) => member.coordinator);
  const coordinatorLabel = group.coordinator
    ? "Ty"
    : coordinator && topicGroupMemberShortName(coordinator);
  const discordUrl =
    discord?.data.status === DiscordStatus.ON_SERVER ? group.discordChannelUrl : undefined;

  const openProfile = (id: string) => {
    onClose();
    router.push({ pathname: "/alumni/[id]", params: { id } });
  };

  return (
    <Modal visible transparent animationType="none" statusBarTranslucent onRequestClose={onClose}>
      <Animated.View entering={FadeIn.duration(150)} style={{ position: "absolute", inset: 0 }}>
        <Pressable accessibilityLabel="Zamknij" onPress={onClose} className="flex-1 bg-black/40" />
      </Animated.View>
      <Animated.View
        key={top === null ? "measuring" : "shown"}
        role="dialog"
        entering={top === null ? undefined : FadeIn.duration(120)}
        className="bg-card absolute overflow-hidden rounded-[20px]"
        style={{
          top: top ?? cardTop,
          maxHeight,
          left: rect.x - 4,
          width: rect.width + 8,
          opacity: top === null ? 0 : 1,
        }}
      >
        <ScrollView bounces={false} nestedScrollEnabled showsVerticalScrollIndicator={false}>
          <View onLayout={onContentLayout} className="gap-3 p-4">
            <View className="flex-row items-center gap-3">
              <View className="flex-1 gap-0.5">
                <Text role="heading" className="text-foreground text-[17px] font-semibold">
                  {group.name}
                </Text>
              </View>
              <Pressable
                role="button"
                accessibilityLabel="Zamknij"
                onPress={onClose}
                hitSlop={8}
                className="active:bg-muted dark:active:bg-input size-10 items-center justify-center rounded-full"
              >
                <X size={22} color={theme.foreground} />
              </Pressable>
            </View>

            {coordinator ? (
              <Pressable
                role="link"
                onPress={() => openProfile(coordinator.id)}
                className="flex-row items-center gap-3 active:opacity-70"
              >
                <InitialsAvatar name={topicGroupMemberName(coordinator)} size="xs" />
                <View className="flex-1">
                  <Text className="text-muted-foreground text-xs">Koordynator</Text>
                  <Text className="text-foreground text-sm font-medium">{coordinatorLabel}</Text>
                </View>
                <ChevronRight size={18} color={theme.mutedForeground} />
              </Pressable>
            ) : null}

            {group.description ? (
              <View>
                <Text className="text-muted-foreground text-sm leading-[22px]">
                  {group.description}
                </Text>
              </View>
            ) : null}

            <View className="border-border -mx-4 overflow-hidden border-y px-4">
              <Pressable
                role="button"
                disabled={!memberList}
                onPress={() => setExpanded(!expanded)}
                className="h-[52px] flex-row items-center gap-2.5"
              >
                <Users size={18} color={theme.mutedForeground} />
                <Text className="text-foreground flex-1 text-sm font-semibold">
                  {topicGroupMemberCountLabel(group.memberCount)}
                </Text>
                {memberList ? (
                  <View className="flex-row items-center gap-0.5">
                    <Text className="text-accent text-[13px] font-medium">
                      {expanded ? "Zwiń" : "Pokaż"}
                    </Text>
                    <View style={{ transform: [{ rotate: expanded ? "180deg" : "0deg" }] }}>
                      <ChevronDown size={16} color={theme.accent} />
                    </View>
                  </View>
                ) : null}
              </Pressable>
              {expanded && memberList ? (
                <View>
                  <ScrollView
                    nestedScrollEnabled
                    showsVerticalScrollIndicator={false}
                    style={{ maxHeight: 188 }}
                    className="pb-1.5"
                  >
                    {memberList.map((member) => (
                      <Pressable
                        key={member.id}
                        role="link"
                        onPress={() => openProfile(member.id)}
                        className="h-11 flex-row items-center gap-3 active:opacity-70"
                      >
                        <InitialsAvatar name={topicGroupMemberName(member)} size="xs" />
                        <Text className="text-foreground flex-1 text-sm font-medium">
                          {member.id === myId ? "Ty" : topicGroupMemberShortName(member)}
                        </Text>
                        {member.coordinator ? (
                          <CoordinatorPill label="Koordynator" />
                        ) : member.id === myId ? (
                          <Pill label="Ty" />
                        ) : null}
                      </Pressable>
                    ))}
                  </ScrollView>
                </View>
              ) : null}
            </View>

            <View>
              {!group.member ? (
                <Button size="lg" onPress={toggle} disabled={isPending} className="h-12">
                  <Text className="font-semibold">Dołącz, żeby zobaczyć Discorda</Text>
                </Button>
              ) : discordUrl ? (
                <Button
                  variant="outline"
                  onPress={() => void Linking.openURL(discordUrl)}
                  className="w-full"
                >
                  <DiscordIcon size={18} color={theme.foreground} />
                  <Text className="font-semibold">Otwórz kanał na Discordzie</Text>
                </Button>
              ) : (
                <DiscordBanner />
              )}
            </View>

            {group.member ? (
              <View>
                {group.coordinator ? (
                  <Text className="text-muted-foreground text-center text-[13px]">
                    Aby odejść, przekaż grupę innemu członkowi na stronie klubu
                  </Text>
                ) : (
                  <Button variant="outline" size="sm" onPress={toggle} disabled={isPending}>
                    <LogOut size={16} color={theme.destructive} />
                    <Text className="font-semibold">Opuść grupę</Text>
                  </Button>
                )}
              </View>
            ) : null}
          </View>
        </ScrollView>
      </Animated.View>
    </Modal>
  );
}
