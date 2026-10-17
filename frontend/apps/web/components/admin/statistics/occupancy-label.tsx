export function OccupancyLabel({ percent }: { percent?: number }) {
  if (percent == null) {
    return <span className="text-muted-foreground">bez limitu</span>;
  }
  if (percent > 100) {
    return (
      <span className="text-destructive font-semibold tabular-nums">
        {percent}%<span className="sr-only"> — ponad limit miejsc</span>
      </span>
    );
  }
  return <span className="text-foreground font-semibold tabular-nums">{percent}%</span>;
}
