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

export function useToggleMembership(group: TopicGroupResponse) {
  const queryClient = useQueryClient();
  const join = useJoinTopicGroup();
  const leave = useLeaveTopicGroup();

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

  return { toggle, isPending: join.isPending || leave.isPending };
}
