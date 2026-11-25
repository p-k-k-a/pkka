import { useIsAlumni } from "@/components/auth/require-alumni";
import { DiscordBanner } from "@/components/topic-groups/discord-banner";
import { AvatarStack, GroupTile, Pill } from "@/components/topic-groups/group-visuals";
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
import { router } from "expo-router";
import { ArrowUpRight, ChevronDown, ChevronUp, MessageCircle, X } from "lucide-react-native";
import { useRef, useState } from "react";
import {
  Linking,
  Modal,
  Pressable,
  ScrollView,
  View,
  useWindowDimensions,
  type LayoutChangeEvent,
} from "react-native";
import Animated, {
  Easing,
  FadeIn,
  FadeInDown,
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withSpring,
  withTiming,
} from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { scheduleOnRN } from "react-native-worklets";

export type CardRect = { x: number; y: number; width: number; height: number };

const OPEN = { duration: 300, easing: Easing.bezier(0.2, 0.9, 0.25, 1) };
const CLOSE = { duration: 200, easing: Easing.bezier(0.4, 0, 0.6, 1) };
const TAB_BAR_HEIGHT = 76;

const stagger = (index: number) => FadeInDown.delay(90 + index * 50).duration(220);

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
  const [shifted, setShifted] = useState(false);
  const opened = useRef(false);

  const maxBottom = windowHeight - insets.bottom - TAB_BAR_HEIGHT - 12;
  const cardTop = rect.y - 4;
  const cardHeight = rect.height + 8;
  const top = useSharedValue(cardTop);
  const height = useSharedValue(cardHeight);
  const scale = useSharedValue(0.96);
  const opacity = useSharedValue(0);
  const backdrop = useSharedValue(0);

  const popoverStyle = useAnimatedStyle(() => ({
    top: top.value,
    height: height.value,
    opacity: opacity.value,
    transform: [{ scale: scale.value }],
  }));
  const backdropStyle = useAnimatedStyle(() => ({ opacity: backdrop.value }));

  const onContentLayout = (event: LayoutChangeEvent) => {
    const contentHeight = event.nativeEvent.layout.height;
    const finalTop = Math.max(
      insets.top + 12,
      Math.min(opened.current ? top.value : cardTop, maxBottom - contentHeight),
    );
    top.value = withTiming(finalTop, OPEN);
    height.value = withTiming(Math.min(contentHeight, maxBottom - finalTop), OPEN);
    if (opened.current) return;
    opened.current = true;
    setShifted(finalTop < cardTop);
    scale.value = withSpring(1, { damping: 14, stiffness: 260, mass: 0.8 });
    opacity.value = withTiming(1, { duration: 90 });
    backdrop.value = withTiming(1, { duration: 200 });
  };

  const close = () => {
    top.value = withTiming(cardTop, CLOSE);
    height.value = withTiming(cardHeight, CLOSE);
    scale.value = withTiming(0.96, CLOSE);
    backdrop.value = withTiming(0, { duration: 200 });
    opacity.value = withDelay(
      60,
      withTiming(0, { duration: 160 }, (finished) => {
        if (finished) scheduleOnRN(onClose);
      }),
    );
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
    <Modal visible transparent animationType="none" statusBarTranslucent onRequestClose={close}>
      <Animated.View style={[{ position: "absolute", inset: 0 }, backdropStyle]}>
        <Pressable accessibilityLabel="Zamknij" onPress={close} className="bg-band/30 flex-1" />
      </Animated.View>
      <Animated.View
        role="dialog"
        className="bg-card absolute overflow-hidden rounded-[20px]"
        style={[
          { left: rect.x - 4, width: rect.width + 8, transformOrigin: shifted ? "bottom" : "top" },
          popoverStyle,
        ]}
      >
        <ScrollView bounces={false} nestedScrollEnabled>
          <View onLayout={onContentLayout} className="gap-3 p-4">
            <View className="flex-row items-center gap-3">
              <GroupTile name={group.name} />
              <View className="flex-1 gap-0.5">
                <Text role="heading" className="text-foreground text-[17px] font-semibold">
                  {group.name}
                </Text>
                {coordinatorLabel ? (
                  <Text className="text-muted-foreground text-xs">
                    Koordynator: {coordinatorLabel}
                  </Text>
                ) : null}
              </View>
              <Pressable
                role="button"
                accessibilityLabel="Zamknij"
                onPress={close}
                hitSlop={8}
                className="bg-muted dark:bg-input size-9 items-center justify-center rounded-full active:opacity-70"
              >
                <X size={18} color={theme.mutedForeground} />
              </Pressable>
            </View>

            {group.description ? (
              <Animated.View entering={stagger(0)}>
                <Text className="text-muted-foreground text-sm leading-[22px]">
                  {group.description}
                </Text>
              </Animated.View>
            ) : null}

            <Animated.View entering={stagger(1)} className="border-border -mx-4 border-y px-4">
              <Pressable
                role="button"
                disabled={!memberList}
                onPress={() => setExpanded((open) => !open)}
                className="h-[52px] flex-row items-center gap-2.5"
              >
                {memberList ? <AvatarStack members={memberList} size="xs" /> : null}
                <Text className="text-foreground flex-1 text-sm font-semibold">
                  {topicGroupMemberCountLabel(group.memberCount)}
                </Text>
                {memberList ? (
                  <View className="flex-row items-center gap-0.5">
                    <Text className="text-accent text-[13px] font-medium">
                      {expanded ? "Zwiń" : "Pokaż"}
                    </Text>
                    {expanded ? (
                      <ChevronUp size={16} color={theme.accent} />
                    ) : (
                      <ChevronDown size={16} color={theme.accent} />
                    )}
                  </View>
                ) : null}
              </Pressable>
              {expanded && memberList ? (
                <Animated.View entering={FadeIn.delay(80).duration(200)}>
                  <ScrollView nestedScrollEnabled style={{ maxHeight: 188 }} className="pb-1.5">
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
                          <Pill label="Koordynator" amber />
                        ) : member.id === myId ? (
                          <Pill label="Ty" />
                        ) : null}
                      </Pressable>
                    ))}
                  </ScrollView>
                </Animated.View>
              ) : null}
            </Animated.View>

            <Animated.View entering={stagger(2)}>
              {!group.member ? (
                <Button size="lg" onPress={toggle} disabled={isPending} className="h-12">
                  <Text className="font-semibold">Dołącz, żeby zobaczyć Discorda</Text>
                </Button>
              ) : discordUrl ? (
                <Pressable
                  role="link"
                  onPress={() => void Linking.openURL(discordUrl)}
                  className="bg-band flex-row items-center gap-3 rounded-[14px] py-2.5 pl-3 pr-2.5 active:opacity-80"
                >
                  <View className="size-9 items-center justify-center rounded-[10px] bg-white/10">
                    <MessageCircle size={18} color={theme.bandForeground} />
                  </View>
                  <View className="flex-1">
                    <Text className="text-band-foreground text-sm font-semibold">
                      Serwer Discord
                    </Text>
                    <Text className="text-band-foreground/70 text-xs">Otwórz kanał grupy</Text>
                  </View>
                  <View className="bg-primary size-11 items-center justify-center rounded-full">
                    <ArrowUpRight size={20} color={theme.primaryForeground} />
                  </View>
                </Pressable>
              ) : (
                <DiscordBanner />
              )}
            </Animated.View>

            {group.member && !group.coordinator ? (
              <Pressable
                role="button"
                onPress={toggle}
                disabled={isPending}
                className="h-8 items-center justify-center self-center active:opacity-70"
              >
                <Text className="text-muted-foreground text-[13px] font-medium">Opuść grupę</Text>
              </Pressable>
            ) : null}
          </View>
        </ScrollView>
      </Animated.View>
    </Modal>
  );
}
