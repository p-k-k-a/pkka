import { Crown } from "lucide-react";
import { cn } from "@/lib/utils";

export function CoordinatorBadge({ label, className }: { label: string; className?: string }) {
  return (
    <span
      className={cn(
        "border-primary bg-primary/15 text-foreground inline-flex shrink-0 items-center gap-1 rounded-md border px-2 py-0.5 text-xs font-semibold",
        className,
      )}
    >
      <Crown className="size-3" aria-hidden="true" />
      {label}
    </span>
  );
}
