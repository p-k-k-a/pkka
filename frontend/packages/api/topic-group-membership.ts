import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "./mutator";
import {
  getGetTopicGroupQueryKey,
  getListTopicGroupMembersQueryKey,
  getListTopicGroupMembersQueryOptions,
  getListTopicGroupsQueryKey,
  useJoinTopicGroup,
  useLeaveTopicGroup,
} from "./src/generated/api";
import type { TopicGroupResponse } from "./src/generated/schemas";

type MembershipErrorHandler = (joining: boolean, status: number | undefined) => void;

/** Joins or leaves `group`, then refreshes every query that shows its membership. */
export function useToggleTopicGroupMembership(
  group: TopicGroupResponse,
  onError: MembershipErrorHandler,
) {
  const queryClient = useQueryClient();
  const join = useJoinTopicGroup();
  const leave = useLeaveTopicGroup();
  // Own flag instead of the mutations' isPending: it also covers the refresh, so the button
  // stays disabled until the new membership is on screen.
  const [isPending, setIsPending] = useState(false);

  const toggleMembership = async () => {
    if (isPending) return;
    setIsPending(true);
    const joining = !group.member;
    let joined = false;
    try {
      await (joining ? join : leave).mutateAsync({ id: group.id });
      joined = joining;
    } catch (error) {
      onError(joining, error instanceof ApiError ? error.status : undefined);
    }
    // Load the members before the group flips to "member", so the list never shows up empty.
    if (joined) {
      await queryClient.fetchQuery(getListTopicGroupMembersQueryOptions(group.id)).catch(() => {});
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: getListTopicGroupsQueryKey() }),
      queryClient.invalidateQueries({ queryKey: getGetTopicGroupQueryKey(group.slug) }),
      joined
        ? undefined
        : queryClient.invalidateQueries({ queryKey: getListTopicGroupMembersQueryKey(group.id) }),
    ]);
    setIsPending(false);
  };

  return { toggleMembership, isPending };
}
