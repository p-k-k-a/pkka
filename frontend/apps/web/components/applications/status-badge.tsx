import { ApplicationStatus, TopicGroupRequestStatus } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { statusLabel } from "@pkka/domain";

type ReviewStatus = ApplicationStatus | TopicGroupRequestStatus;

const STATUS_VARIANT: Record<ReviewStatus, "default" | "secondary" | "destructive"> = {
  [ApplicationStatus.UNDER_REVIEW]: "secondary",
  [ApplicationStatus.APPROVED]: "default",
  [ApplicationStatus.REJECTED]: "destructive",
};

type StatusBadgeProps = {
  status: ReviewStatus;
  label?: string;
};

export function StatusBadge({ status, label = statusLabel(status) }: StatusBadgeProps) {
  return (
    <Badge variant={STATUS_VARIANT[status]} className="rounded-lg uppercase">
      {label}
    </Badge>
  );
}
