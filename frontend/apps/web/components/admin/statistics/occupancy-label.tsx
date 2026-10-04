import { TriangleAlert } from "lucide-react";

export function OccupancyLabel({ percent }: { percent?: number }) {
  if (percent == null) {
    return <span className="text-muted-foreground">bez limitu</span>;
  }
  if (percent > 100) {
    return (
      <span className="text-destructive inline-flex items-center gap-1 font-semibold">
        <TriangleAlert className="size-3.5" aria-hidden />
        {percent}%<span className="sr-only"> — ponad limit miejsc</span>
      </span>
    );
  }
  return <span className="text-foreground font-semibold tabular-nums">{percent}%</span>;
}
