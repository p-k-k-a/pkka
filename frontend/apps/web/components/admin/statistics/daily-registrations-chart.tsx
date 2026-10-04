"use client";

import { useId, useState, type KeyboardEvent, type PointerEvent } from "react";
import { format, parseISO } from "date-fns";
import { pl } from "date-fns/locale";
import type { DailyRegistrationsResponse } from "@pkka/api";
import { pluralPl } from "@pkka/domain";
import { cn } from "@/lib/utils";

const PLOT_HEIGHT = 220;
const MAX_TICKS = 6;
const MIN_LABEL_GAP = 18;
const MAX_DAYS_WITH_GAPS = 45;

function dayLabel(date: string, pattern: string) {
  return format(parseISO(date), pattern, { locale: pl });
}

function signUpsLabel(count: number) {
  return `${count} ${pluralPl(count, "zapis", "zapisy", "zapisów")}`;
}

function cancellationsLabel(count: number) {
  return `${count} ${pluralPl(count, "wypis", "wypisy", "wypisów")}`;
}

function tickIndices(count: number) {
  if (count <= MAX_TICKS) {
    return Array.from({ length: count }, (_, index) => index);
  }
  const steps = MAX_TICKS - 1;
  return [
    ...new Set(Array.from({ length: MAX_TICKS }, (_, i) => Math.round((i * (count - 1)) / steps))),
  ];
}

function barHeight(value: number, max: number) {
  if (value === 0 || max === 0) return "0px";
  return `max(2px, ${(value / max) * 100}%)`;
}

function edgeTransform(index: number, count: number) {
  const position = (index + 0.5) / count;
  if (position < 0.15) return "translateX(0)";
  if (position > 0.85) return "translateX(-100%)";
  return "translateX(-50%)";
}

function edgeLeft(index: number, count: number) {
  const position = (index + 0.5) / count;
  if (position < 0.15) return `${(index / count) * 100}%`;
  if (position > 0.85) return `${((index + 1) / count) * 100}%`;
  return `${position * 100}%`;
}

type DailyRegistrationsChartProps = {
  days: DailyRegistrationsResponse[];
};

export function DailyRegistrationsChart({ days }: DailyRegistrationsChartProps) {
  const [active, setActive] = useState<number | null>(null);
  const [announce, setAnnounce] = useState(false);
  const descriptionId = useId();

  const maxUp = Math.max(0, ...days.map((day) => day.signUps));
  const maxDown = Math.max(0, ...days.map((day) => day.cancellations));
  const totalSignUps = days.reduce((sum, day) => sum + day.signUps, 0);
  const totalCancellations = days.reduce((sum, day) => sum + day.cancellations, 0);

  if (days.length === 0 || maxUp + maxDown === 0) {
    return (
      <div className="border-border text-muted-foreground flex h-40 items-center justify-center rounded-lg border border-dashed px-4 text-center text-sm">
        Brak zapisów i wypisów w tym okresie.
      </div>
    );
  }

  const upHeight = Math.round((PLOT_HEIGHT * maxUp) / (maxUp + maxDown));
  const downHeight = PLOT_HEIGHT - upHeight;
  const ticks = tickIndices(days.length);
  const activeDay = active === null ? null : days[active];

  const pickFromPointer = (event: PointerEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    if (rect.width === 0) return;
    const index = Math.floor(((event.clientX - rect.left) / rect.width) * days.length);
    setActive(Math.min(days.length - 1, Math.max(0, index)));
  };

  const moveWithKeys = (event: KeyboardEvent<HTMLDivElement>) => {
    const last = days.length - 1;
    const current = active ?? last;
    const next =
      event.key === "ArrowLeft"
        ? Math.max(0, current - 1)
        : event.key === "ArrowRight"
          ? Math.min(last, current + 1)
          : event.key === "Home"
            ? 0
            : event.key === "End"
              ? last
              : null;
    if (next === null) return;
    event.preventDefault();
    setAnnounce(true);
    setActive(next);
  };

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

      <div className="flex gap-2">
        <div
          className="text-muted-foreground relative w-7 shrink-0 text-right text-xs tabular-nums"
          style={{ height: PLOT_HEIGHT }}
          aria-hidden
        >
          {maxUp > 0 && upHeight >= MIN_LABEL_GAP ? (
            <span className="absolute top-0 right-0 -translate-y-1/2">{maxUp}</span>
          ) : null}
          <span className="absolute right-0 -translate-y-1/2" style={{ top: upHeight }}>
            0
          </span>
          {maxDown > 0 && downHeight >= MIN_LABEL_GAP ? (
            <span className="absolute right-0 bottom-0 translate-y-1/2">{maxDown}</span>
          ) : null}
        </div>

        <div className="@container min-w-0 flex-1">
          <div
            role="group"
            tabIndex={0}
            aria-roledescription="wykres"
            aria-label="Zapisy i wypisy dzień po dniu. Strzałki w lewo i w prawo zmieniają dzień."
            aria-describedby={descriptionId}
            className="focus-visible:ring-ring/50 relative cursor-crosshair rounded-sm outline-none focus-visible:ring-3"
            style={{ height: PLOT_HEIGHT }}
            onPointerMove={(event) => {
              setAnnounce(false);
              pickFromPointer(event);
            }}
            onPointerLeave={() => setActive(null)}
            onFocus={() => setActive((current) => current ?? days.length - 1)}
            onBlur={() => setActive(null)}
            onKeyDown={moveWithKeys}
          >
            <div className="border-border absolute inset-x-0 border-t" style={{ top: upHeight }} />
            <div
              className={cn(
                "absolute inset-0 flex overflow-hidden",
                days.length <= MAX_DAYS_WITH_GAPS && "gap-[2px]",
              )}
            >
              {days.map((day, index) => (
                <div
                  key={day.date}
                  className={cn(
                    "flex min-w-0 flex-1 flex-col items-center transition-opacity",
                    active !== null && active !== index && "opacity-45",
                  )}
                >
                  <div className="flex w-full justify-center" style={{ height: upHeight }}>
                    <div className="flex h-full w-full max-w-6 flex-col justify-end">
                      <div
                        className="bg-chart-1 w-full rounded-t-[4px]"
                        style={{ height: barHeight(day.signUps, maxUp) }}
                      />
                    </div>
                  </div>
                  <div className="flex w-full justify-center" style={{ height: downHeight }}>
                    <div className="flex h-full w-full max-w-6 flex-col justify-start">
                      <div
                        className="bg-chart-2 w-full rounded-b-[4px]"
                        style={{ height: barHeight(day.cancellations, maxDown) }}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {activeDay && active !== null ? (
              <div
                className="bg-popover text-popover-foreground ring-foreground/10 pointer-events-none absolute -top-2 z-10 min-w-36 rounded-lg px-3 py-2 text-xs shadow-md ring-1"
                style={{
                  left: edgeLeft(active, days.length),
                  transform: `${edgeTransform(active, days.length)} translateY(-100%)`,
                }}
              >
                <p className="text-muted-foreground mb-1 first-letter:uppercase">
                  {dayLabel(activeDay.date, "EEEE, d MMMM")}
                </p>
                <p className="flex items-center gap-2">
                  <span className="bg-chart-1 h-0.5 w-3" aria-hidden />
                  <span className="text-foreground font-semibold tabular-nums">
                    {activeDay.signUps}
                  </span>
                  <span className="text-muted-foreground">
                    {pluralPl(activeDay.signUps, "zapis", "zapisy", "zapisów")}
                  </span>
                </p>
                <p className="flex items-center gap-2">
                  <span className="bg-chart-2 h-0.5 w-3" aria-hidden />
                  <span className="text-foreground font-semibold tabular-nums">
                    {activeDay.cancellations}
                  </span>
                  <span className="text-muted-foreground">
                    {pluralPl(activeDay.cancellations, "wypis", "wypisy", "wypisów")}
                  </span>
                </p>
              </div>
            ) : null}
          </div>

          <div className="text-muted-foreground relative mt-2 h-4 text-xs" aria-hidden>
            {ticks.map((index) => (
              <span
                key={index}
                className={cn(
                  "absolute whitespace-nowrap",
                  index === 0 && index !== days.length - 1 && "hidden @3xs:inline",
                  index !== 0 && index !== days.length - 1 && "hidden @md:inline",
                )}
                style={{
                  left: edgeLeft(index, days.length),
                  transform: edgeTransform(index, days.length),
                }}
              >
                {dayLabel(days[index].date, "d MMM")}
              </span>
            ))}
          </div>
        </div>
      </div>

      <p id={descriptionId} className="sr-only" aria-live={announce ? "polite" : "off"}>
        {activeDay
          ? `${dayLabel(activeDay.date, "EEEE, d MMMM")}: ${signUpsLabel(activeDay.signUps)}, ${cancellationsLabel(activeDay.cancellations)}.`
          : `Od ${dayLabel(days[0].date, "d MMMM")} do ${dayLabel(days[days.length - 1].date, "d MMMM")}: ${signUpsLabel(totalSignUps)}, ${cancellationsLabel(totalCancellations)}.`}
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
              </tr>
            </thead>
            <tbody>
              {days.map((day) => (
                <tr key={day.date} className="border-border border-t">
                  <td className="text-foreground px-4 py-2">{dayLabel(day.date, "d MMMM yyyy")}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{day.signUps}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{day.cancellations}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </div>
  );
}
