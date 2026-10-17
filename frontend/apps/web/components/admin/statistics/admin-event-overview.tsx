"use client";

import { useEffect, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { differenceInCalendarDays, parseISO } from "date-fns";
import { ArrowLeft, CalendarClock, MapPin, Pencil, Users } from "lucide-react";
import {
  type ApiError,
  useGetAdminEvent,
  useGetEventStatistics,
  type EventStatisticsResponse,
} from "@pkka/api";
import {
  audienceLabel,
  eventLocationLabel,
  formatDateTime,
  formatEventDateLong,
  formatTimeRange,
  isAdmin,
  pluralPl,
} from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { DailyRegistrationsChart } from "@/components/admin/statistics/daily-registrations-chart";
import { OccupancyMeter } from "@/components/admin/statistics/occupancy-meter";
import { StatTile } from "@/components/admin/statistics/stat-tile";

const ADMIN_EVENTS_PATH = "/dashboard/admin/events";

function startStatus(startsAt: string, endsAt: string) {
  const now = new Date();
  const start = parseISO(startsAt);
  if (start > now) {
    const days = differenceInCalendarDays(start, now);
    if (days === 0) return "dziś";
    if (days === 1) return "jutro";
    return `za ${days} dni`;
  }
  return parseISO(endsAt) > now ? "trwa teraz" : "zakończone";
}

function seatsHint(percent?: number) {
  if (percent == null) return "bez limitu miejsc";
  if (percent > 100) return `${percent}% miejsc, limit obniżono po zapisach`;
  if (percent === 100) return "100% miejsc, komplet";
  return `${percent}% miejsc zajętych`;
}

function waitlistHint(stats: EventStatisticsResponse) {
  return pluralPl(
    stats.waitlisted,
    "osoba czeka na miejsce",
    "osoby czekają na miejsce",
    "osób czeka na miejsce",
  );
}

function SummaryRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="border-border flex items-baseline justify-between gap-4 border-t py-3 first:border-t-0 first:pt-0">
      <dt className="text-muted-foreground text-sm">{label}</dt>
      <dd className="text-foreground text-right text-sm font-medium">{children}</dd>
    </div>
  );
}

function OverviewSkeleton() {
  return (
    <div className="px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px] space-y-6">
        <Skeleton className="h-10 w-80 rounded-lg" />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-32 rounded-xl" />
          ))}
        </div>
        <Skeleton className="h-80 w-full rounded-xl" />
      </div>
    </div>
  );
}

export function AdminEventOverview({ id }: { id: string }) {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const eventQuery = useGetAdminEvent(id, { query: { enabled: admin } });
  const statsQuery = useGetEventStatistics(id, { query: { enabled: admin } });

  if (isLoading || !admin || eventQuery.isLoading) {
    return <OverviewSkeleton />;
  }

  const event = eventQuery.data?.data;
  if (eventQuery.isError || !event) {
    const apiError = eventQuery.error as unknown as ApiError | null;
    const notFound = apiError?.status === 404;
    return (
      <div className="px-4 py-16 text-center">
        <h1 className="font-heading text-foreground mb-2 text-[28px] font-semibold">
          {notFound ? "Nie znaleziono wydarzenia" : "Nie udało się załadować wydarzenia"}
        </h1>
        <p className="text-muted-foreground mb-8">
          {notFound
            ? "To wydarzenie nie istnieje albo zostało usunięte."
            : "Spróbuj odświeżyć stronę za chwilę."}
        </p>
        <Button asChild variant="outline">
          <Link href={ADMIN_EVENTS_PATH}>
            <ArrowLeft data-icon="inline-start" />
            Wróć do listy wydarzeń
          </Link>
        </Button>
      </div>
    );
  }

  const stats = statsQuery.data?.data;
  const seriesEndsAtStart = parseISO(event.startsAt) <= new Date();

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

          <div className="flex flex-wrap items-start justify-between gap-6">
            <div className="min-w-0 space-y-3">
              <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
                Statystyki wydarzenia
              </p>
              <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight break-words md:text-[33px]">
                {event.title}
              </h1>
              <ul className="text-muted-foreground flex flex-wrap items-center gap-x-5 gap-y-2 text-sm">
                <li className="flex items-center gap-1.5">
                  <CalendarClock className="size-4 shrink-0" aria-hidden />
                  <span>
                    {formatEventDateLong(event.startsAt)},{" "}
                    {formatTimeRange(event.startsAt, event.endsAt)}
                    <span className="text-foreground font-medium">
                      {" "}
                      ({startStatus(event.startsAt, event.endsAt)})
                    </span>
                  </span>
                </li>
                <li className="flex items-center gap-1.5">
                  <MapPin className="size-4 shrink-0" aria-hidden />
                  {eventLocationLabel(event.type, event.location)}
                </li>
                <li className="flex items-center gap-1.5">
                  <Users className="size-4 shrink-0" aria-hidden />
                  {audienceLabel(event.audience)}
                </li>
              </ul>
            </div>
            <Button asChild variant="outline" size="lg">
              <Link href={`${ADMIN_EVENTS_PATH}/${event.id}/edit`}>
                <Pencil data-icon="inline-start" />
                Edytuj wydarzenie
              </Link>
            </Button>
          </div>
        </div>

        {statsQuery.isLoading ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-32 rounded-xl" />
            ))}
          </div>
        ) : statsQuery.isError || !stats ? (
          <p className="text-destructive font-medium">Nie udało się załadować statystyk.</p>
        ) : (
          <>
            <section
              aria-label="Najważniejsze liczby"
              className="grid grid-cols-1 gap-4 sm:grid-cols-3"
            >
              <StatTile
                label="Zapisani"
                value={
                  stats.seatLimit == null
                    ? stats.registered
                    : `${stats.registered}/${stats.seatLimit}`
                }
                hint={seatsHint(stats.occupancyPercent)}
              >
                {stats.occupancyPercent == null ? null : (
                  <OccupancyMeter
                    className="mt-1"
                    percent={stats.occupancyPercent}
                    label="Obłożenie wydarzenia"
                  />
                )}
              </StatTile>
              <StatTile
                label="Lista rezerwowa"
                value={stats.waitlisted}
                hint={waitlistHint(stats)}
              />
              <StatTile label="Wypisy" value={stats.cancellations} hint="rezygnacje z zapisu" />
            </section>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <Card className="gap-6 px-6 py-6 lg:col-span-2">
                <div className="space-y-1">
                  <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight">
                    Zapisy w czasie
                  </h2>
                  <p className="text-muted-foreground text-sm">
                    Od {formatEventDateLong(stats.daily[0]?.date ?? stats.publishedAt)} do{" "}
                    {seriesEndsAtStart ? "startu wydarzenia" : "dziś"}.
                  </p>
                </div>
                <DailyRegistrationsChart days={stats.daily} seatLimit={stats.seatLimit} />
              </Card>

              <Card className="gap-4 px-6 py-6">
                <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight">
                  Podsumowanie
                </h2>
                <dl>
                  <SummaryRow label="Wszystkie zapisy">{stats.signUps}</SummaryRow>
                  <SummaryRow label="Limit miejsc">{stats.seatLimit ?? "brak"}</SummaryRow>
                  <SummaryRow label="Opublikowano">{formatDateTime(stats.publishedAt)}</SummaryRow>
                  <SummaryRow label="Zapisy do">
                    {formatDateTime(event.registrationClosesAt ?? event.startsAt)}
                  </SummaryRow>
                </dl>
              </Card>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
