import { cn } from "@/lib/utils";

type OccupancyMeterProps = {
  percent: number;
  label: string;
  className?: string;
};

export function OccupancyMeter({ percent, label, className }: OccupancyMeterProps) {
  const filled = Math.min(100, Math.max(0, percent));
  return (
    <div
      role="meter"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.min(percent, 100)}
      aria-valuetext={`${percent}%`}
      className={cn("bg-chart-1/15 h-2 w-full overflow-hidden rounded-full", className)}
    >
      <div className="bg-chart-1 h-full rounded-full" style={{ width: `${filled}%` }} />
    </div>
  );
}
