import { DiscordBanner } from "@/components/topic-groups/discord-banner";
import { CoordinatorPill, MutedPill } from "@/components/topic-groups/pills";
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
import { useEffect, useState } from "react";
import { Linking, Modal, Pressable, ScrollView, View } from "react-native";
import Animated, {
  FadeIn,
  FadeInDown,
  useAnimatedStyle,
  useSharedValue,
  withSpring,
  withTiming,
} from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";

const stagger = (index: number) => FadeInDown.delay(60 + index * 50).duration(220);

type GroupPopoverProps = {
  group: TopicGroupResponse;
  onClose: () => void;
};

export function GroupPopover({ group, onClose }: GroupPopoverProps) {
  const theme = useThemeColors();
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { data: me } = useMe({ query: { enabled: !!user } });
  const canSeeMembers = group.member || isAdmin(me?.data.roles);
  const { data: membersResponse } = useListTopicGroupMembers(group.id, {
    query: { enabled: canSeeMembers },
  });
  const { data: profileResponse } = useGetMyProfile({ query: { enabled: canSeeMembers } });
  const { data: discordStatusResponse } = useGetMyDiscordStatus({
    query: { enabled: group.member },
  });
  const { toggleMembership, isPending } = useToggleMembership(group);
  const [expanded, setExpanded] = useState(false);
  const popoverScale = useSharedValue(0.96);
  const popoverOpacity = useSharedValue(0);
  const backdropOpacity = useSharedValue(0);
  const chevronRotation = useSharedValue(0);
  const popoverStyle = useAnimatedStyle(() => ({
    opacity: popoverOpacity.value,
    transform: [{ scale: popoverScale.value }],
  }));
  const backdropStyle = useAnimatedStyle(() => ({ opacity: backdropOpacity.value }));
  const chevronStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${chevronRotation.value}deg` }],
  }));

  useEffect(() => {
    popoverScale.value = withSpring(1, { damping: 14, stiffness: 260, mass: 0.8 });
    popoverOpacity.value = withTiming(1, { duration: 120 });
    backdropOpacity.value = withTiming(1, { duration: 200 });
  }, [popoverScale, popoverOpacity, backdropOpacity]);

  const toggleMembers = () => {
    chevronRotation.value = withTiming(expanded ? 0 : 180, { duration: 260 });
    setExpanded(!expanded);
  };
  const memberList =
    canSeeMembers && membersResponse?.status === 200 ? membersResponse.data : undefined;
  const myId = profileResponse?.status === 200 ? profileResponse.data.id : undefined;
  const coordinator = memberList?.find((member) => member.coordinator);
  const coordinatorLabel = group.coordinator
    ? "Ty"
    : coordinator && topicGroupMemberShortName(coordinator);
  const discordUrl =
    discordStatusResponse?.data.status === DiscordStatus.ON_SERVER
      ? group.discordChannelUrl
      : undefined;

  const openProfile = (id: string) => {
    onClose();
    router.push({ pathname: "/alumni/[id]", params: { id } });
  };

  return (
    <Modal visible transparent animationType="none" statusBarTranslucent onRequestClose={onClose}>
      <Animated.View style={[{ position: "absolute", inset: 0 }, backdropStyle]}>
        <Pressable accessibilityLabel="Zamknij" onPress={onClose} className="flex-1 bg-black/40" />
      </Animated.View>
      <View
        pointerEvents="box-none"
        className="flex-1 justify-center px-4"
        style={{ paddingTop: insets.top + 16, paddingBottom: insets.bottom + 16 }}
      >
        <Animated.View
          role="dialog"
          style={popoverStyle}
          className="bg-card max-h-full overflow-hidden rounded-[20px]"
        >
          <ScrollView bounces={false} nestedScrollEnabled showsVerticalScrollIndicator={false}>
            {/* Remounts on join/leave so the content fades in again with the new membership. */}
            <Animated.View
              key={String(group.member)}
              entering={FadeIn.duration(180)}
              className="gap-3 p-4"
            >
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
                <Animated.View entering={stagger(0)}>
                  <Text className="text-muted-foreground text-sm leading-[22px]">
                    {group.description}
                  </Text>
                </Animated.View>
              ) : null}

              <Animated.View
                entering={stagger(1)}
                className="border-border -mx-4 overflow-hidden border-y px-4"
              >
                <Pressable
                  role="button"
                  disabled={!memberList}
                  onPress={toggleMembers}
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
                      <Animated.View style={chevronStyle}>
                        <ChevronDown size={16} color={theme.accent} />
                      </Animated.View>
                    </View>
                  ) : null}
                </Pressable>
                {expanded && memberList ? (
                  <Animated.View entering={FadeIn.delay(80).duration(200)}>
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
                            <MutedPill label="Ty" />
                          ) : null}
                        </Pressable>
                      ))}
                    </ScrollView>
                  </Animated.View>
                ) : null}
              </Animated.View>

              <Animated.View entering={stagger(2)}>
                {!group.member ? (
                  <Button
                    size="lg"
                    onPress={toggleMembership}
                    disabled={isPending}
                    className="h-12"
                  >
                    <Text className="font-semibold">
                      {isPending ? "Dołączanie..." : "Dołącz, żeby zobaczyć Discorda"}
                    </Text>
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
              </Animated.View>

              {group.member ? (
                <View>
                  {group.coordinator ? (
                    <Text className="text-muted-foreground text-center text-[13px]">
                      Aby odejść, przekaż grupę innemu członkowi na stronie klubu
                    </Text>
                  ) : (
                    <Button
                      variant="outline"
                      size="sm"
                      onPress={toggleMembership}
                      disabled={isPending}
                    >
                      <LogOut size={16} color={theme.destructive} />
                      <Text className="font-semibold">
                        {isPending ? "Opuszczanie..." : "Opuść grupę"}
                      </Text>
                    </Button>
                  )}
                </View>
              ) : null}
            </Animated.View>
          </ScrollView>
        </Animated.View>
      </View>
    </Modal>
  );
}
