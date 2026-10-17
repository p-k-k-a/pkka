"use client";

import { format, parseISO } from "date-fns";
import { pl } from "date-fns/locale";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
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
  const totalSignUps = days.reduce((sum, day) => sum + day.signUps, 0);
  const totalCancellations = days.reduce((sum, day) => sum + day.cancellations, 0);

  if (days.length === 0 || totalSignUps + totalCancellations === 0) {
    return (
      <div className="border-border text-muted-foreground flex h-40 items-center justify-center rounded-lg border border-dashed px-4 text-center text-sm">
        Brak zapisów i wypisów w tym okresie.
      </div>
    );
  }

  return (
    <div className="space-y-4">
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

      <div className="-ml-2" style={{ height: CHART_HEIGHT }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={days}
            barGap={2}
            barCategoryGap="20%"
            margin={{ top: 8, right: 8, bottom: 0, left: 0 }}
            title="Zapisy i wypisy dzień po dniu"
          >
            <CartesianGrid vertical={false} stroke={GRID} strokeOpacity={0.6} />
            <XAxis
              dataKey="date"
              tickLine={false}
              tick={{ fill: AXIS_TEXT, fontSize: 12 }}
              axisLine={{ stroke: GRID }}
              tickFormatter={(date: string) => dayLabel(date, "d MMM")}
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
              cursor={{ fill: "var(--color-muted)", opacity: 0.6 }}
              content={({ active, payload }) => <ChartTooltip active={active} payload={payload} />}
            />
            <Bar
              dataKey="signUps"
              name="Zapisy"
              fill={SIGN_UPS}
              radius={[4, 4, 0, 0]}
              maxBarSize={20}
            />
            <Bar
              dataKey="cancellations"
              name="Wypisy"
              fill={CANCELLATIONS}
              radius={[4, 4, 0, 0]}
              maxBarSize={20}
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
