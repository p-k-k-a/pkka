"use client";

import { toast } from "sonner";
import { useToggleTopicGroupMembership, type TopicGroupResponse } from "@pkka/api";
import { topicGroupMembershipErrorMessage } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const LABEL = { join: "Dołącz", leave: "Opuść" };
const PENDING_LABEL = { join: "Dołączanie…", leave: "Opuszczanie…" };

type MembershipButtonProps = {
  group: TopicGroupResponse;
  prominent?: boolean;
  className?: string;
};

/** Callers don't render it for the coordinator, who can't leave the group. */
export function MembershipButton({ group, prominent, className }: MembershipButtonProps) {
  const { toggleMembership, isPending } = useToggleTopicGroupMembership(group, (joining, status) =>
    toast.error(topicGroupMembershipErrorMessage(status, joining)),
  );
  const action = group.member ? "leave" : "join";

  return (
    <Button
      type="button"
      size={prominent ? "xl" : "sm"}
      variant={group.member ? "outline" : "default"}
      disabled={isPending}
      onClick={toggleMembership}
      className={cn("font-semibold", className)}
    >
      {isPending ? PENDING_LABEL[action] : LABEL[action]}
    </Button>
  );
}
