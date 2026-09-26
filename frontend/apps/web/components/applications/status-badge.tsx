import { ApplicationStatus } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { statusLabel } from "@pkka/domain";

const STATUS_VARIANT: Record<ApplicationStatus, "default" | "secondary" | "destructive"> = {
  [ApplicationStatus.UNDER_REVIEW]: "secondary",
  [ApplicationStatus.APPROVED]: "default",
  [ApplicationStatus.REJECTED]: "destructive",
};

export function StatusBadge({ status }: { status: ApplicationStatus }) {
  return (
    <Badge variant={STATUS_VARIANT[status]} className="rounded-lg uppercase">
      {statusLabel(status)}
    </Badge>
  );
}
