import { occupancyPercent } from "@/components/admin/statistics/occupancy-meter";

export function OccupancyLabel({
  registered,
  seatLimit,
}: {
  registered: number;
  seatLimit?: number;
}) {
  if (seatLimit == null) {
    return <span className="text-muted-foreground">bez limitu</span>;
  }
  const percent = occupancyPercent(registered, seatLimit);
  if (registered > seatLimit) {
    return (
      <span className="text-destructive font-semibold tabular-nums">
        {percent}%<span className="sr-only">, ponad limit miejsc</span>
      </span>
    );
  }
  return <span className="text-foreground font-semibold tabular-nums">{percent}%</span>;
}
