import { TopicGroupRequestStatus } from "@pkka/api";
import { topicGroupRequestStatusLabel } from "@pkka/domain";
import { Badge } from "@/components/ui/badge";

const STATUS_VARIANT: Record<TopicGroupRequestStatus, "default" | "secondary" | "destructive"> = {
  [TopicGroupRequestStatus.UNDER_REVIEW]: "secondary",
  [TopicGroupRequestStatus.APPROVED]: "default",
  [TopicGroupRequestStatus.REJECTED]: "destructive",
};

export function RequestStatusBadge({ status }: { status: TopicGroupRequestStatus }) {
  return (
    <Badge variant={STATUS_VARIANT[status]} className="rounded-lg uppercase">
      {topicGroupRequestStatusLabel(status)}
    </Badge>
  );
}
