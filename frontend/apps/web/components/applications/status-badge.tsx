import { ApplicationStatus } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { statusLabel } from "@pkka/domain";

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive"> = {
  [ApplicationStatus.UNDER_REVIEW]: "secondary",
  [ApplicationStatus.APPROVED]: "default",
  [ApplicationStatus.REJECTED]: "destructive",
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <Badge variant={STATUS_VARIANT[status] ?? "secondary"} className="rounded-lg uppercase">
      {statusLabel(status)}
    </Badge>
  );
}
