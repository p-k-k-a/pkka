import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import {
  ApiError,
  getGetTopicGroupQueryKey,
  getListTopicGroupMembersQueryKey,
  getListTopicGroupsQueryKey,
  useJoinTopicGroup,
  useLeaveTopicGroup,
  type TopicGroupResponse,
} from "@pkka/api";
import { topicGroupMembershipErrorMessage } from "@pkka/domain";
import { useQueryClient } from "@tanstack/react-query";
import { Alert } from "react-native";

type MembershipButtonProps = {
  group: TopicGroupResponse;
  prominent?: boolean;
  className?: string;
};

export function MembershipButton({ group, prominent, className }: MembershipButtonProps) {
  const queryClient = useQueryClient();
  const join = useJoinTopicGroup();
  const leave = useLeaveTopicGroup();
  const isPending = join.isPending || leave.isPending;

  const toggle = async () => {
    const joining = !group.member;
    try {
      await (joining ? join : leave).mutateAsync({ id: group.id });
    } catch (error) {
      Alert.alert(
        joining ? "Nie dołączono do grupy" : "Nie opuszczono grupy",
        topicGroupMembershipErrorMessage(
          error instanceof ApiError ? error.status : undefined,
          joining,
        ),
      );
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: getListTopicGroupsQueryKey() }),
      queryClient.invalidateQueries({ queryKey: getGetTopicGroupQueryKey(group.slug) }),
      queryClient.invalidateQueries({ queryKey: getListTopicGroupMembersQueryKey(group.id) }),
    ]);
  };

  if (group.coordinator) return null;

  return (
    <Button
      variant={group.member ? (prominent ? "outline" : "ghost") : prominent ? "default" : "outline"}
      size="sm"
      onPress={toggle}
      disabled={isPending}
      className={className}
    >
      <Text className="font-semibold">
        {group.member
          ? isPending
            ? "Opuszczanie…"
            : "Opuść"
          : isPending
            ? "Dołączanie…"
            : "Dołącz"}
      </Text>
    </Button>
  );
}
