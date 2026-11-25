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
  LinearTransition,
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withSequence,
  withSpring,
  withTiming,
} from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { scheduleOnRN } from "react-native-worklets";

export type CardRect = { x: number; y: number; width: number; height: number };

const OPEN = { duration: 300, easing: Easing.bezier(0.2, 0.9, 0.25, 1) };
const CLOSE = { duration: 200, easing: Easing.bezier(0.4, 0, 0.6, 1) };
const TAB_BAR_HEIGHT = 76;

const RESIZE = LinearTransition.duration(300).easing(OPEN.easing);
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
  const [frame, setFrame] = useState<{ top: number; height: number } | null>(null);
  const [settled, setSettled] = useState(false);
  const closing = useRef(false);

  const maxBottom = windowHeight - insets.bottom - TAB_BAR_HEIGHT - 12;
  const cardTop = rect.y - 4;
  const cardHeight = rect.height + 8;
  const shifted = !!frame && frame.top < cardTop;
  // The window wraps its content, so it resizes in the same layout pass as the content.
  const maxHeight = maxBottom - (frame?.top ?? cardTop);
  const reveal = useSharedValue(0);
  const scale = useSharedValue(0.96);
  const opacity = useSharedValue(0);
  const backdrop = useSharedValue(0);
  const chevron = useSharedValue(0);
  const listOpacity = useSharedValue(1);
  const collapsing = useRef(false);

  const clipStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
    transform: [{ translateY: reveal.value }, { scale: scale.value }],
  }));
  const contentStyle = useAnimatedStyle(() => ({ transform: [{ translateY: -reveal.value }] }));
  const backdropStyle = useAnimatedStyle(() => ({ opacity: backdrop.value }));
  const chevronStyle = useAnimatedStyle(() => ({ transform: [{ rotate: `${chevron.value}deg` }] }));
  const listStyle = useAnimatedStyle(() => ({ opacity: listOpacity.value }));

  const hiddenOffset = (next: { top: number; height: number }) =>
    (next.top < cardTop ? 1 : -1) * Math.max(0, next.height - cardHeight);

  const onContentLayout = (event: LayoutChangeEvent) => {
    const contentHeight = event.nativeEvent.layout.height;
    const top = Math.max(insets.top + 12, Math.min(cardTop, maxBottom - contentHeight));
    const next = { top, height: Math.min(contentHeight, maxBottom - top) };
    setFrame(next);
    if (frame) return;
    reveal.value = withSequence(
      withTiming(hiddenOffset(next), { duration: 0 }),
      withTiming(0, OPEN, (finished) => {
        if (finished) scheduleOnRN(setSettled, true);
      }),
    );
    scale.value = withSpring(1, { damping: 14, stiffness: 260, mass: 0.8 });
    opacity.value = withTiming(1, { duration: 90 });
    backdrop.value = withTiming(1, { duration: 200 });
  };

  const close = () => {
    if (closing.current) return;
    closing.current = true;
    if (!frame) return onClose();
    reveal.value = withTiming(hiddenOffset(frame), CLOSE);
    scale.value = withTiming(0.96, CLOSE);
    backdrop.value = withTiming(0, { duration: 200 });
    opacity.value = withDelay(
      60,
      withTiming(0, { duration: 160 }, (finished) => {
        if (finished) scheduleOnRN(onClose);
      }),
    );
  };

  const toggleMembers = () => {
    if (collapsing.current) return;
    if (!expanded) {
      listOpacity.value = 1;
      chevron.value = withTiming(180, { duration: 260 });
      setExpanded(true);
      return;
    }
    // Fade the list out in place first, so the resize below starts in one layout pass.
    collapsing.current = true;
    chevron.value = withTiming(0, { duration: 260 });
    listOpacity.value = withTiming(0, { duration: 140 }, (finished) => {
      if (finished) scheduleOnRN(finishCollapse);
    });
  };

  const finishCollapse = () => {
    collapsing.current = false;
    setExpanded(false);
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

  const resize = settled ? RESIZE : undefined;

  return (
    <Modal visible transparent animationType="none" statusBarTranslucent onRequestClose={close}>
      <Animated.View style={[{ position: "absolute", inset: 0 }, backdropStyle]}>
        <Pressable accessibilityLabel="Zamknij" onPress={close} className="flex-1 bg-black/40" />
      </Animated.View>
      <Animated.View
        role="dialog"
        layout={resize}
        className="absolute overflow-hidden rounded-[20px]"
        style={[
          {
            top: frame?.top ?? cardTop,
            maxHeight,
            left: rect.x - 4,
            width: rect.width + 8,
            transformOrigin: shifted ? "bottom" : "top",
          },
          clipStyle,
        ]}
      >
        <Animated.View
          layout={resize}
          className="bg-card overflow-hidden rounded-[20px]"
          style={[{ maxHeight }, contentStyle]}
        >
          <Animated.ScrollView
            layout={resize}
            bounces={false}
            nestedScrollEnabled
            showsVerticalScrollIndicator={false}
          >
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
                  onPress={close}
                  hitSlop={8}
                  className="bg-muted dark:bg-input size-9 items-center justify-center rounded-full active:opacity-70"
                >
                  <X size={18} color={theme.mutedForeground} />
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
                layout={resize}
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
                  <Animated.View entering={FadeIn.delay(80).duration(200)} style={listStyle}>
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
                  </Animated.View>
                ) : null}
              </Animated.View>

              <Animated.View entering={stagger(2)} layout={resize}>
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
              </Animated.View>

              {group.member ? (
                <Animated.View layout={resize}>
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
                </Animated.View>
              ) : null}
            </View>
          </Animated.ScrollView>
        </Animated.View>
      </Animated.View>
    </Modal>
  );
}
