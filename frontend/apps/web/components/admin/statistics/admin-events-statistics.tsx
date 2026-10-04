"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowDown, ArrowLeft } from "lucide-react";
import { EventTimeframe, useListEventOccupancy, type EventOccupancyResponse } from "@pkka/api";
import { formatEventDateShort, isAdmin, pluralPl } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/lib/auth-context";
import { cn } from "@/lib/utils";
import { OccupancyLabel } from "@/components/admin/statistics/occupancy-label";
import { OccupancyMeter } from "@/components/admin/statistics/occupancy-meter";
import { StatTile } from "@/components/admin/statistics/stat-tile";

const ADMIN_EVENTS_PATH = "/dashboard/admin/events";

type SortKey = "date" | "occupancy";

function sortRows(rows: EventOccupancyResponse[], sortKey: SortKey) {
  if (sortKey === "date") return rows;
  return [...rows].sort((a, b) => {
    if (a.occupancyPercent == null) return b.occupancyPercent == null ? 0 : 1;
    if (b.occupancyPercent == null) return -1;
    return b.occupancyPercent - a.occupancyPercent;
  });
}

function averageOccupancy(rows: EventOccupancyResponse[]) {
  const limited = rows.filter((row) => row.occupancyPercent != null);
  if (limited.length === 0) return null;
  const sum = limited.reduce((total, row) => total + (row.occupancyPercent ?? 0), 0);
  return { percent: Math.round(sum / limited.length), events: limited.length };
}

function SortHeader({
  label,
  sortKey,
  active,
  onSort,
  className,
}: {
  label: string;
  sortKey: SortKey;
  active: SortKey;
  onSort: (key: SortKey) => void;
  className?: string;
}) {
  const isActive = active === sortKey;
  return (
    <th
      scope="col"
      aria-sort={isActive ? "descending" : "none"}
      className={cn("px-4 py-3", className)}
    >
      <button
        type="button"
        onClick={() => onSort(sortKey)}
        className={cn(
          "hover:text-foreground inline-flex items-center gap-1 font-semibold tracking-widest uppercase",
          isActive && "text-foreground",
        )}
      >
        {label}
        <ArrowDown className={cn("size-3.5", !isActive && "opacity-0")} aria-hidden />
      </button>
    </th>
  );
}

export function AdminEventsStatistics() {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const [timeframe, setTimeframe] = useState<EventTimeframe>(EventTimeframe.ALL);
  const [sortKey, setSortKey] = useState<SortKey>("date");

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const {
    data: response,
    isLoading: isListLoading,
    isError,
    isFetching,
  } = useListEventOccupancy(
    { timeframe },
    { query: { enabled: admin, placeholderData: (previous) => previous } },
  );

  if (isLoading || !admin) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <Skeleton className="mx-auto h-40 w-full max-w-3xl rounded-lg" />
      </div>
    );
  }

  const rows = response?.data ?? [];
  const sorted = sortRows(rows, sortKey);
  const registered = rows.reduce((total, row) => total + row.registered, 0);
  const waitlisted = rows.reduce((total, row) => total + row.waitlisted, 0);
  const cancellations = rows.reduce((total, row) => total + row.cancellations, 0);
  const average = averageOccupancy(rows);

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px] space-y-10">
        <div>
          <Button asChild variant="ghost" size="sm" className="mb-8 -ml-2">
            <Link href={ADMIN_EVENTS_PATH}>
              <ArrowLeft data-icon="inline-start" />
              Wróć do listy wydarzeń
            </Link>
          </Button>
          <div className="space-y-6">
            <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
              Statystyki
            </p>
            <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              Porównanie obłożenia wydarzeń
            </h1>
            <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
              Ile miejsc zajęto na każdym wydarzeniu, kto czeka na liście rezerwowej i ile osób się
              wypisało. Kliknij wydarzenie, aby zobaczyć zapisy dzień po dniu.
            </p>
            <Tabs
              value={timeframe}
              onValueChange={(value) => setTimeframe(value as EventTimeframe)}
            >
              <TabsList>
                <TabsTrigger value={EventTimeframe.ALL}>Wszystkie</TabsTrigger>
                <TabsTrigger value={EventTimeframe.UPCOMING}>Nadchodzące</TabsTrigger>
                <TabsTrigger value={EventTimeframe.PAST}>Minione</TabsTrigger>
              </TabsList>
            </Tabs>
          </div>
        </div>

        {isListLoading ? (
          <div className="space-y-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-32 rounded-xl" />
              ))}
            </div>
            <Skeleton className="h-80 w-full rounded-xl" />
          </div>
        ) : isError ? (
          <p className="text-destructive font-medium">Nie udało się załadować statystyk.</p>
        ) : rows.length === 0 ? (
          <p className="text-muted-foreground">
            {timeframe === EventTimeframe.ALL ? "Brak wydarzeń." : "Brak wydarzeń w tym zakresie."}
          </p>
        ) : (
          <div className={cn("space-y-6 transition-opacity", isFetching && "opacity-60")}>
            <section
              aria-label="Podsumowanie wybranych wydarzeń"
              className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
            >
              <StatTile
                label="Zapisani"
                value={registered}
                hint={`na ${rows.length} ${pluralPl(rows.length, "wydarzeniu", "wydarzeniach", "wydarzeniach")}`}
              />
              <StatTile
                label="Lista rezerwowa"
                value={waitlisted}
                hint={pluralPl(waitlisted, "osoba czeka", "osoby czekają", "osób czeka")}
              />
              <StatTile
                label="Wypisy"
                value={cancellations}
                hint="rezygnacje z miejsca i z kolejki"
              />
              <StatTile
                label="Średnie obłożenie"
                value={average ? `${average.percent}%` : "—"}
                hint={
                  average
                    ? `z ${average.events} ${pluralPl(average.events, "wydarzenia", "wydarzeń", "wydarzeń")} z limitem miejsc`
                    : "żadne wydarzenie nie ma limitu miejsc"
                }
              />
            </section>

            <Card className="gap-0 py-0">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <caption className="sr-only">
                    Obłożenie wydarzeń, posortowane{" "}
                    {sortKey === "date" ? "od najnowszych" : "od najpełniejszych"}
                  </caption>
                  <thead className="bg-muted text-muted-foreground text-xs">
                    <tr>
                      <SortHeader
                        label="Wydarzenie"
                        sortKey="date"
                        active={sortKey}
                        onSort={setSortKey}
                      />
                      <SortHeader
                        label="Obłożenie"
                        sortKey="occupancy"
                        active={sortKey}
                        onSort={setSortKey}
                        className="w-[32%]"
                      />
                      <th
                        scope="col"
                        className="px-4 py-3 text-right font-semibold tracking-widest uppercase"
                      >
                        Zapisani
                      </th>
                      <th
                        scope="col"
                        className="px-4 py-3 text-right font-semibold tracking-widest uppercase"
                      >
                        Rezerwa
                      </th>
                      <th
                        scope="col"
                        className="px-4 py-3 text-right font-semibold tracking-widest uppercase"
                      >
                        Wypisy
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {sorted.map((row) => (
                      <tr key={row.eventId} className="border-border hover:bg-muted/50 border-t">
                        <td className="px-4 py-3">
                          <Link
                            href={`${ADMIN_EVENTS_PATH}/${row.eventId}`}
                            className="text-foreground hover:text-accent font-semibold hover:underline"
                          >
                            {row.title}
                          </Link>
                          <p className="text-muted-foreground text-xs">
                            {formatEventDateShort(row.startsAt)}
                          </p>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            {row.occupancyPercent == null ? (
                              <div className="flex-1" />
                            ) : (
                              <OccupancyMeter
                                className="flex-1"
                                percent={row.occupancyPercent}
                                label={`Obłożenie: ${row.title}`}
                              />
                            )}
                            <span className="w-20 shrink-0 text-right">
                              <OccupancyLabel percent={row.occupancyPercent} />
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-right tabular-nums">
                          <span className="text-foreground font-semibold">{row.registered}</span>
                          <span className="text-muted-foreground">
                            {row.seatLimit == null ? "" : ` / ${row.seatLimit}`}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right tabular-nums">{row.waitlisted}</td>
                        <td className="px-4 py-3 text-right tabular-nums">{row.cancellations}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}
