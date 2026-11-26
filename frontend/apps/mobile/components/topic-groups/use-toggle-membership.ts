import { useToggleTopicGroupMembership, type TopicGroupResponse } from "@pkka/api";
import { topicGroupMembershipErrorMessage } from "@pkka/domain";
import { Alert } from "react-native";

export function useToggleMembership(group: TopicGroupResponse) {
  return useToggleTopicGroupMembership(group, (joining, status) =>
    Alert.alert(
      joining ? "Nie dołączono do grupy" : "Nie opuszczono grupy",
      topicGroupMembershipErrorMessage(status, joining),
    ),
  );
}
