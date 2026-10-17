"use client";

import { useId, useState } from "react";
import { format, parseISO } from "date-fns";
import { pl } from "date-fns/locale";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { DailyRegistrationsResponse } from "@pkka/api";
import { pluralPl } from "@pkka/domain";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

const CHART_HEIGHT = 260;
const SIGN_UPS = "var(--color-chart-1)";
const CANCELLATIONS = "var(--color-chart-2)";
const GRID = "var(--color-border)";
const AXIS_TEXT = "var(--color-muted-foreground)";

type View = "daily" | "total";

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
  view,
}: {
  active?: boolean;
  payload?: ReadonlyArray<{ payload?: unknown }>;
  view: View;
}) {
  const day = payload?.[0]?.payload as DailyRegistrationsResponse | undefined;
  if (!active || !day) return null;
  return (
    <div className="bg-popover text-popover-foreground ring-foreground/10 min-w-40 space-y-1 rounded-lg px-3 py-2 text-xs shadow-md ring-1">
      <p className="text-muted-foreground first-letter:uppercase">
        {dayLabel(day.date, "EEEE, d MMMM")}
      </p>
      {view === "daily" ? (
        <>
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
        </>
      ) : (
        <TooltipRow
          color={SIGN_UPS}
          value={day.signedUpAtEndOfDay}
          label={pluralPl(
            day.signedUpAtEndOfDay,
            "osoba zapisana",
            "osoby zapisane",
            "osób zapisanych",
          )}
        />
      )}
    </div>
  );
}

const axisProps = {
  tickLine: false,
  tick: { fill: AXIS_TEXT, fontSize: 12 },
} as const;

type DailyRegistrationsChartProps = {
  days: DailyRegistrationsResponse[];
  seatLimit?: number;
};

export function DailyRegistrationsChart({ days, seatLimit }: DailyRegistrationsChartProps) {
  const [view, setView] = useState<View>("daily");
  const gradientId = useId();

  const totalSignUps = days.reduce((sum, day) => sum + day.signUps, 0);
  const totalCancellations = days.reduce((sum, day) => sum + day.cancellations, 0);

  if (days.length === 0 || totalSignUps + totalCancellations === 0) {
    return (
      <div className="border-border text-muted-foreground flex h-40 items-center justify-center rounded-lg border border-dashed px-4 text-center text-sm">
        Brak zapisów i wypisów w tym okresie.
      </div>
    );
  }

  const xAxis = (
    <XAxis
      dataKey="date"
      {...axisProps}
      axisLine={{ stroke: GRID }}
      tickFormatter={(date: string) => dayLabel(date, "d MMM")}
      minTickGap={24}
      tickMargin={8}
    />
  );
  const yAxis = (
    <YAxis {...axisProps} axisLine={false} allowDecimals={false} width={32} tickMargin={4} />
  );
  const grid = <CartesianGrid vertical={false} stroke={GRID} strokeOpacity={0.6} />;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        {view === "daily" ? (
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
        ) : (
          <p className="text-muted-foreground text-sm">
            Liczba osób zapisanych na koniec dnia, razem z listą rezerwową.
          </p>
        )}
        <Tabs value={view} onValueChange={(value) => setView(value as View)}>
          <TabsList>
            <TabsTrigger value="daily">Dziennie</TabsTrigger>
            <TabsTrigger value="total">Łącznie</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      <div className="-ml-2" style={{ height: CHART_HEIGHT }}>
        <ResponsiveContainer width="100%" height="100%">
          {view === "daily" ? (
            <BarChart
              data={days}
              barGap={2}
              barCategoryGap="20%"
              margin={{ top: 8, right: 8, bottom: 0, left: 0 }}
              title="Zapisy i wypisy dzień po dniu"
            >
              {grid}
              {xAxis}
              {yAxis}
              <Tooltip
                cursor={{ fill: "var(--color-muted)", opacity: 0.6 }}
                content={({ active, payload }) => (
                  <ChartTooltip active={active} payload={payload} view="daily" />
                )}
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
          ) : (
            <AreaChart
              data={days}
              margin={{ top: 8, right: 8, bottom: 0, left: 0 }}
              title="Osoby zapisane na koniec każdego dnia"
            >
              <defs>
                <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={SIGN_UPS} stopOpacity={0.25} />
                  <stop offset="100%" stopColor={SIGN_UPS} stopOpacity={0} />
                </linearGradient>
              </defs>
              {grid}
              {xAxis}
              {yAxis}
              {seatLimit != null ? (
                <ReferenceLine
                  y={seatLimit}
                  stroke={AXIS_TEXT}
                  strokeDasharray="4 4"
                  ifOverflow="extendDomain"
                  label={{
                    value: `Limit miejsc: ${seatLimit}`,
                    position: "insideTopLeft",
                    fill: AXIS_TEXT,
                    fontSize: 12,
                  }}
                />
              ) : null}
              <Tooltip
                cursor={{ stroke: GRID }}
                content={({ active, payload }) => (
                  <ChartTooltip active={active} payload={payload} view="total" />
                )}
              />
              <Area
                type="stepAfter"
                dataKey="signedUpAtEndOfDay"
                name="Zapisani łącznie"
                stroke={SIGN_UPS}
                strokeWidth={2}
                fill={`url(#${gradientId})`}
                activeDot={{ r: 4, strokeWidth: 2, stroke: "var(--color-card)" }}
              />
            </AreaChart>
          )}
        </ResponsiveContainer>
      </div>

      <p className="sr-only">
        {`Od ${dayLabel(days[0].date, "d MMMM")} do ${dayLabel(days[days.length - 1].date, "d MMMM")}: ${signUpsLabel(totalSignUps)}, ${cancellationsLabel(totalCancellations)}.`}
      </p>

      <details className="group text-sm">
        <summary className="text-accent cursor-pointer font-medium select-none hover:underline">
          Pokaż dane w tabeli
        </summary>
        <div className="border-border mt-3 max-h-72 overflow-y-auto rounded-lg border">
          <table className="w-full text-left">
            <thead className="bg-muted text-muted-foreground sticky top-0 text-xs tracking-widest uppercase">
              <tr>
                <th scope="col" className="px-4 py-2 font-semibold">
                  Dzień
                </th>
                <th scope="col" className="px-4 py-2 text-right font-semibold">
                  Zapisy
                </th>
                <th scope="col" className="px-4 py-2 text-right font-semibold">
                  Wypisy
                </th>
                <th scope="col" className="px-4 py-2 text-right font-semibold">
                  Łącznie
                </th>
              </tr>
            </thead>
            <tbody>
              {days.map((day) => (
                <tr key={day.date} className="border-border border-t">
                  <td className="text-foreground px-4 py-2">{dayLabel(day.date, "d MMMM yyyy")}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{day.signUps}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{day.cancellations}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{day.signedUpAtEndOfDay}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </div>
  );
}
