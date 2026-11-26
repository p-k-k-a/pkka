"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  ApiError,
  getGetTopicGroupQueryKey,
  getListTopicGroupMembersQueryKey,
  getListTopicGroupMembersQueryOptions,
  getListTopicGroupsQueryKey,
  useJoinTopicGroup,
  useLeaveTopicGroup,
  type TopicGroupResponse,
} from "@pkka/api";
import { topicGroupMembershipErrorMessage } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type MembershipButtonProps = {
  group: TopicGroupResponse;
  prominent?: boolean;
  className?: string;
};

export function MembershipButton({ group, prominent, className }: MembershipButtonProps) {
  const queryClient = useQueryClient();
  const join = useJoinTopicGroup();
  const leave = useLeaveTopicGroup();
  const [isPending, setIsPending] = useState(false);

  const toggle = async () => {
    if (isPending) return;
    setIsPending(true);
    const joining = !group.member;
    try {
      await (joining ? join : leave).mutateAsync({ id: group.id });
    } catch (error) {
      toast.error(
        topicGroupMembershipErrorMessage(
          error instanceof ApiError ? error.status : undefined,
          joining,
        ),
      );
    }
    if (joining) {
      await queryClient.fetchQuery(getListTopicGroupMembersQueryOptions(group.id)).catch(() => {});
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: getListTopicGroupsQueryKey() }),
      queryClient.invalidateQueries({ queryKey: getGetTopicGroupQueryKey(group.slug) }),
      queryClient.invalidateQueries({ queryKey: getListTopicGroupMembersQueryKey(group.id) }),
    ]);
    setIsPending(false);
  };

  if (group.coordinator) return null;

  return (
    <Button
      type="button"
      size={prominent ? "xl" : "sm"}
      variant={group.member ? "outline" : "default"}
      disabled={isPending}
      onClick={toggle}
      className={cn(
        "font-semibold",
        group.member && !prominent && "bg-background dark:bg-background border-transparent",
        className,
      )}
    >
      {group.member ? (isPending ? "Opuszczanie…" : "Opuść") : isPending ? "Dołączanie…" : "Dołącz"}
    </Button>
  );
}
