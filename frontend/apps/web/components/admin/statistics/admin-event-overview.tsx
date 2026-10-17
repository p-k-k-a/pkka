"use client";

import { useEffect, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { differenceInCalendarDays, parseISO } from "date-fns";
import { ArrowLeft, Pencil } from "lucide-react";
import { type ApiError, useGetAdminEvent, useGetEventStatistics } from "@pkka/api";
import {
  audienceLabel,
  eventLocationLabel,
  formatDateTime,
  formatEventDateLong,
  formatTimeRange,
  isAdmin,
} from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { DailyRegistrationsChart } from "@/components/admin/statistics/daily-registrations-chart";
import { OccupancyMeter, occupancyPercent } from "@/components/admin/statistics/occupancy-meter";
import { SignupSources } from "@/components/admin/statistics/signup-sources";
import { StatTile } from "@/components/admin/statistics/stat-tile";
import { EventRegistrants } from "@/components/admin/registrants/event-registrants";

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

function EventDetail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="border-border flex items-baseline justify-between gap-4 border-t py-3 first:border-t-0 first:pt-0 last:pb-0">
      <dt className="text-muted-foreground shrink-0 text-sm">{label}</dt>
      <dd className="text-foreground text-right text-sm font-medium break-words">{children}</dd>
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
            <div className="min-w-0 flex-1 space-y-6">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <h1 className="font-heading text-foreground min-w-0 text-[28px] font-semibold tracking-tight break-words md:text-[33px]">
                  {event.title}
                </h1>
                <Button asChild variant="outline" className="shrink-0">
                  <Link href={`${ADMIN_EVENTS_PATH}/${event.id}/edit`}>
                    <Pencil data-icon="inline-start" />
                    Edytuj wydarzenie
                  </Link>
                </Button>
              </div>
            </div>
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
                hint={stats.seatLimit == null ? "bez limitu miejsc" : undefined}
                info="Osoby z miejscem / limit miejsc."
              >
                {stats.seatLimit == null ? null : (
                  <OccupancyMeter
                    className="mt-1"
                    percent={occupancyPercent(stats.registered, stats.seatLimit) ?? 0}
                    label="Obłożenie wydarzenia"
                  />
                )}
                {stats.seatLimit != null && stats.registered > stats.seatLimit ? (
                  <p className="text-destructive text-sm font-medium">
                    {stats.registered - stats.seatLimit} ponad limit
                  </p>
                ) : null}
              </StatTile>
              <StatTile
                label="Lista rezerwowa"
                value={stats.waitlisted}
                info="Osoby czekające na wolne miejsce. Dostają je automatycznie, w kolejności zapisu."
              />
              <StatTile
                label="Wypisy"
                value={stats.cancellations}
                info="Rezygnacje osób z miejscem. Bez zejść z listy rezerwowej."
              />
            </section>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <Card className="flex flex-col gap-6 px-6 py-6 lg:col-span-2">
                <div className="space-y-1">
                  <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight">
                    Zapisy w czasie
                  </h2>
                  <p className="text-muted-foreground text-sm">
                    Od {formatEventDateLong(stats.daily[0]?.date ?? event.createdAt)} do{" "}
                    {seriesEndsAtStart ? "startu wydarzenia" : "dziś"}.
                  </p>
                </div>
                <DailyRegistrationsChart days={stats.daily} />
              </Card>

              <div className="flex flex-col gap-6">
                <Card className="gap-4 px-6 py-6">
                  <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight">
                    Szczegóły
                  </h2>
                  <dl>
                    <EventDetail label="Termin">
                      <span className="block">
                        {formatEventDateLong(event.startsAt)},{" "}
                        {formatTimeRange(event.startsAt, event.endsAt)}
                      </span>
                      <span className="text-muted-foreground block font-normal">
                        {startStatus(event.startsAt, event.endsAt)}
                      </span>
                    </EventDetail>
                    <EventDetail label="Miejsce">
                      {eventLocationLabel(event.type, event.location)}
                    </EventDetail>
                    <EventDetail label="Odbiorcy">{audienceLabel(event.audience)}</EventDetail>
                    <EventDetail label="Opublikowano">
                      {formatDateTime(event.createdAt)}
                    </EventDetail>
                    <EventDetail label="Zapisy do">
                      {formatDateTime(event.registrationClosesAt ?? event.startsAt)}
                    </EventDetail>
                    <EventDetail label="Autor">{event.authorDisplayName ?? "nieznany"}</EventDetail>
                  </dl>
                </Card>
                <SignupSources signUps={stats.signUps} />
              </div>
            </div>
          </>
        )}

        <EventRegistrants eventId={event.id} eventTitle={event.title} />
      </div>
    </div>
  );
}
