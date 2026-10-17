import { cn } from "@/lib/utils";

export function occupancyPercent(registered: number, seatLimit?: number) {
  if (seatLimit == null) return undefined;
  if (seatLimit === 0) return registered === 0 ? 100 : registered * 100;
  return Math.round((registered / seatLimit) * 100);
}

type OccupancyMeterProps = {
  percent: number;
  label: string;
  className?: string;
};

export function OccupancyMeter({ percent, label, className }: OccupancyMeterProps) {
  const filled = Math.min(100, Math.max(0, percent));
  const over = percent > 100;
  return (
    <div
      role="meter"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.min(percent, 100)}
      aria-valuetext={`${percent}%`}
      className={cn(
        "h-2 w-full overflow-hidden rounded-full",
        over ? "bg-destructive/15" : "bg-chart-1/15",
        className,
      )}
    >
      <div
        className={cn("h-full rounded-full", over ? "bg-destructive" : "bg-chart-1")}
        style={{ width: `${filled}%` }}
      />
    </div>
  );
}
