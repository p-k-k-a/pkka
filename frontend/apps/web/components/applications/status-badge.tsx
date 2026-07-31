import { ApplicationResponseStatus } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { statusLabel } from "@/lib/application-labels";

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive"> = {
  [ApplicationResponseStatus.UNDER_REVIEW]: "secondary",
  [ApplicationResponseStatus.APPROVED]: "default",
  [ApplicationResponseStatus.REJECTED]: "destructive",
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <Badge variant={STATUS_VARIANT[status] ?? "secondary"} className="rounded-lg uppercase">
      {statusLabel(status)}
    </Badge>
  );
}
