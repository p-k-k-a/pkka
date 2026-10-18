"use client";

import { useState } from "react";
import { format, parseISO } from "date-fns";
import { pl } from "date-fns/locale";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Rectangle,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type BarShapeProps,
} from "recharts";
import type { DailyRegistrationsResponse } from "@pkka/api";
import { pluralPl } from "@pkka/domain";

const CHART_HEIGHT = 260;
const SIGN_UPS = "var(--color-chart-1)";
const CANCELLATIONS = "var(--color-chart-2)";
const GRID = "var(--color-border)";
const AXIS_TEXT = "var(--color-muted-foreground)";

function dayLabel(date: string, pattern: string) {
  return format(parseISO(date), pattern, { locale: pl });
}

function signUpsLabel(count: number) {
  return `${count} ${pluralPl(count, "zapis", "zapisy", "zapisów")}`;
}

function cancellationsLabel(count: number) {
  return `${count} ${pluralPl(count, "wypis", "wypisy", "wypisów")}`;
}

function TooltipRow({ color, value, label }: { color: string; value: number; label: string }) {
  return (
    <p className="flex items-center gap-2">
      <span className="size-2.5 rounded-[2px]" style={{ backgroundColor: color }} aria-hidden />
      <span className="text-foreground font-semibold tabular-nums">{value}</span>
      <span className="text-muted-foreground">{label}</span>
    </p>
  );
}

function ChartTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: ReadonlyArray<{ payload?: unknown }>;
}) {
  const day = payload?.[0]?.payload as DailyRegistrationsResponse | undefined;
  if (!active || !day) return null;
  return (
    <div className="bg-popover text-popover-foreground ring-foreground/10 min-w-40 space-y-1 rounded-lg px-3 py-2 text-xs shadow-md ring-1">
      <p className="text-muted-foreground first-letter:uppercase">
        {dayLabel(day.date, "EEEE, d MMMM")}
      </p>
      <TooltipRow
        color={SIGN_UPS}
        value={day.signUps}
        label={pluralPl(day.signUps, "zapis", "zapisy", "zapisów")}
      />
      <TooltipRow
        color={CANCELLATIONS}
        value={day.cancellations}
        label={pluralPl(day.cancellations, "wypis", "wypisy", "wypisów")}
      />
    </div>
  );
}

export function DailyRegistrationsChart({ days }: { days: DailyRegistrationsResponse[] }) {
  const [activeIndex, setActiveIndex] = useState<number | null>(null);
  const totalSignUps = days.reduce((sum, day) => sum + day.signUps, 0);
  const totalCancellations = days.reduce((sum, day) => sum + day.cancellations, 0);

  if (days.length === 0 || totalSignUps + totalCancellations === 0) {
    return (
      <div className="border-border text-muted-foreground flex min-h-40 flex-1 items-center justify-center rounded-lg border border-dashed px-4 text-center text-sm">
        Brak zapisów i wypisów w tym okresie.
      </div>
    );
  }

  const segment = (kind: "signUps" | "cancellations") =>
    function Segment(props: BarShapeProps) {
      const day = days[props.index];
      const onTop = kind === "cancellations" || day.cancellations === 0;
      const gap = kind === "cancellations" && day.signUps > 0 ? 2 : 0;
      const height = props.height - gap;
      if (height <= 0) return <g />;
      return (
        <Rectangle
          {...props}
          height={height}
          radius={onTop ? [4, 4, 0, 0] : 0}
          fillOpacity={activeIndex === null || activeIndex === props.index ? 1 : 0.3}
          className="transition-[fill-opacity] duration-150"
        />
      );
    };
  const signUpsSegment = segment("signUps");
  const cancellationsSegment = segment("cancellations");

  return (
    <div className="flex flex-1 flex-col gap-4">
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
        <span className="text-foreground inline-flex items-center gap-2">
          <span className="bg-chart-1 size-3 rounded-[2px]" aria-hidden />
          Zapisy
          <span className="text-muted-foreground tabular-nums">({totalSignUps})</span>
        </span>
        <span className="text-foreground inline-flex items-center gap-2">
          <span className="bg-chart-2 size-3 rounded-[2px]" aria-hidden />
          Wypisy
          <span className="text-muted-foreground tabular-nums">({totalCancellations})</span>
        </span>
      </div>

      <div className="-ml-2 flex-1" style={{ minHeight: CHART_HEIGHT }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={days}
            barCategoryGap="20%"
            margin={{ top: 8, right: 8, bottom: 0, left: 0 }}
            onMouseMove={(state) => {
              const index = state.activeTooltipIndex;
              setActiveIndex(index == null ? null : Number(index));
            }}
            onMouseLeave={() => setActiveIndex(null)}
          >
            <CartesianGrid vertical={false} stroke={GRID} strokeOpacity={0.6} />
            <XAxis
              dataKey="date"
              tickLine={false}
              tick={{ fill: AXIS_TEXT, fontSize: 12 }}
              axisLine={{ stroke: GRID }}
              tickFormatter={(date: string) => dayLabel(date, "d MMM")}
              interval="preserveStartEnd"
              minTickGap={24}
              tickMargin={8}
            />
            <YAxis
              tickLine={false}
              tick={{ fill: AXIS_TEXT, fontSize: 12 }}
              axisLine={false}
              allowDecimals={false}
              width={32}
              tickMargin={4}
            />
            <Tooltip
              cursor={{ fill: "var(--color-foreground)", fillOpacity: 0.08, radius: 4 }}
              content={({ active, payload }) => <ChartTooltip active={active} payload={payload} />}
            />
            <Bar
              dataKey="signUps"
              name="Zapisy"
              stackId="day"
              fill={SIGN_UPS}
              maxBarSize={24}
              shape={signUpsSegment}
              activeBar={signUpsSegment}
            />
            <Bar
              dataKey="cancellations"
              name="Wypisy"
              stackId="day"
              fill={CANCELLATIONS}
              maxBarSize={24}
              shape={cancellationsSegment}
              activeBar={cancellationsSegment}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <p className="sr-only">
        {`Od ${dayLabel(days[0].date, "d MMMM")} do ${dayLabel(days[days.length - 1].date, "d MMMM")}: ${signUpsLabel(totalSignUps)}, ${cancellationsLabel(totalCancellations)}.`}
      </p>
    </div>
  );
}
