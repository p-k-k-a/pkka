"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, ChartColumn, Pencil, Plus, Trash2 } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { parseISO } from "date-fns";
import {
  getListAdminEventsQueryKey,
  getListEventOccupancyQueryKey,
  getListEventsQueryKey,
  useDeleteAdminEvent,
  useListAdminEvents,
  useListEventOccupancy,
  EventTimeframe,
  type AdminEventSummaryResponse,
  type EventOccupancyResponse,
} from "@pkka/api";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { EventCalendarCard } from "@/components/events/event-calendar-card";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/lib/auth-context";
import { audienceLabel, isAdmin, pluralPl } from "@pkka/domain";
import { OccupancyLabel } from "@/components/admin/statistics/occupancy-label";
import { OccupancyMeter } from "@/components/admin/statistics/occupancy-meter";
import { SignupSources } from "@/components/admin/statistics/signup-sources";
import { StatTile } from "@/components/admin/statistics/stat-tile";

const PAGE_SIZE = 20;

type SortKey = "startsAt,asc" | "startsAt,desc" | "createdAt,desc" | "createdAt,asc";

function defaultSort(timeframe: EventTimeframe): SortKey {
  return timeframe === EventTimeframe.UPCOMING ? "startsAt,asc" : "startsAt,desc";
}
const ADMIN_EVENTS_PATH = "/dashboard/admin/events";

function OccupancySummary({ rows }: { rows: EventOccupancyResponse[] }) {
  const registered = rows.reduce((total, row) => total + row.registered, 0);
  const waitlisted = rows.reduce((total, row) => total + row.waitlisted, 0);
  const cancellations = rows.reduce((total, row) => total + row.cancellations, 0);

  return (
    <section aria-labelledby="upcoming-stats-heading" className="space-y-3">
      <h2 id="upcoming-stats-heading" className="text-foreground text-sm font-semibold">
        Statystyki nadchodzących wydarzeń
      </h2>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatTile
          label="Zapisani"
          value={registered}
          hint={`na ${rows.length} ${pluralPl(rows.length, "wydarzeniu", "wydarzeniach", "wydarzeniach")}`}
          info="Osoby z miejscem."
        />
        <StatTile
          label="Lista rezerwowa"
          value={waitlisted}
          info="Osoby czekające na wolne miejsce."
        />
        <StatTile
          label="Wypisy"
          value={cancellations}
          info="Rezygnacje osób z miejscem. Bez zejść z listy rezerwowej."
        />
      </div>
      <SignupSources signUps={registered} layout="row" />
    </section>
  );
}

function CardOccupancy({ row }: { row: EventOccupancyResponse }) {
  return (
    <div className="space-y-2">
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className="text-foreground font-semibold tabular-nums">
          {row.seatLimit == null
            ? `${row.registered} ${pluralPl(row.registered, "zapisany", "zapisanych", "zapisanych")}`
            : `${row.registered}/${row.seatLimit} miejsc`}
        </span>
        <OccupancyLabel percent={row.occupancyPercent} />
      </div>
      {row.occupancyPercent == null ? null : (
        <OccupancyMeter percent={row.occupancyPercent} label={`Obłożenie: ${row.title}`} />
      )}
      <p className="text-muted-foreground text-xs">
        Lista rezerwowa:{" "}
        <span className="text-foreground font-medium tabular-nums">{row.waitlisted}</span>
      </p>
    </div>
  );
}

export function AdminEventsList() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const [page, setPage] = useState(0);
  const [timeframe, setTimeframe] = useState<EventTimeframe>(EventTimeframe.UPCOMING);
  const [chosenSort, setChosenSort] = useState<SortKey | null>(null);
  const sortKey = chosenSort ?? defaultSort(timeframe);
  const [eventToDelete, setEventToDelete] = useState<AdminEventSummaryResponse | null>(null);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const {
    data: response,
    isLoading: isListLoading,
    isError,
  } = useListAdminEvents(
    {
      timeframe,
      page,
      size: PAGE_SIZE,
      sort: [sortKey],
    },
    { query: { enabled: admin, placeholderData: (previous) => previous } },
  );
  const occupancy = useListEventOccupancy(
    { timeframe: EventTimeframe.ALL },
    { query: { enabled: admin } },
  );

  const deleteEvent = useDeleteAdminEvent({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: getListAdminEventsQueryKey() });
        queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() });
        queryClient.invalidateQueries({ queryKey: getListEventOccupancyQueryKey() });
        setEventToDelete(null);
      },
    },
  });

  if (isLoading || !admin) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <Skeleton className="mx-auto h-40 w-full max-w-3xl rounded-lg" />
      </div>
    );
  }

  const pageData = response?.data;
  const events = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;
  const firstShown = currentPage * (pageData?.size ?? PAGE_SIZE) + 1;
  const lastShown = firstShown + events.length - 1;
  const occupancyRows = occupancy.data?.data ?? [];
  const occupancyById = new Map(occupancyRows.map((row) => [row.eventId, row]));
  const now = new Date();
  const upcomingRows = occupancyRows.filter((row) => parseISO(row.startsAt) > now);

  return (
    <div>
      <section className="bg-background px-4 pt-10 pb-6 md:px-10 md:pt-16 md:pb-8">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <div className="flex flex-wrap items-start justify-between gap-6">
            <div className="space-y-6">
              <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
                Wydarzenia
              </h1>
              <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
                Zarządzaj wydarzeniami klubu i sprawdzaj, kto się na nie zapisał.
              </p>
            </div>
            <Button asChild className="shrink-0">
              <Link href={`${ADMIN_EVENTS_PATH}/new`}>
                <Plus data-icon="inline-start" />
                Nowe wydarzenie
              </Link>
            </Button>
          </div>
          {occupancy.isLoading ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-28 rounded-xl" />
              ))}
            </div>
          ) : occupancy.isError ? null : (
            <OccupancySummary rows={upcomingRows} />
          )}
        </div>
      </section>

      <section className="bg-background px-4 pb-10 md:px-10 md:pb-16">
        <div className="mx-auto max-w-[1280px] space-y-8">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <Tabs
              value={timeframe}
              onValueChange={(value) => {
                setTimeframe(value as EventTimeframe);
                setPage(0);
              }}
            >
              <TabsList>
                <TabsTrigger value={EventTimeframe.UPCOMING}>Nadchodzące</TabsTrigger>
                <TabsTrigger value={EventTimeframe.PAST}>Archiwum</TabsTrigger>
              </TabsList>
            </Tabs>
            <label className="text-muted-foreground flex items-center gap-2 text-sm whitespace-nowrap">
              Sortuj według
              <Select
                className="h-9 w-auto"
                value={sortKey}
                onChange={(changeEvent) => {
                  setChosenSort(changeEvent.target.value as SortKey);
                  setPage(0);
                }}
              >
                <option value="startsAt,asc">daty wydarzenia, od najstarszej</option>
                <option value="startsAt,desc">daty wydarzenia, od najnowszej</option>
                <option value="createdAt,desc">daty publikacji, od najnowszej</option>
                <option value="createdAt,asc">daty publikacji, od najstarszej</option>
              </Select>
            </label>
          </div>
          {isListLoading ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="bg-muted h-72 w-full rounded-none" />
              ))}
            </div>
          ) : isError ? (
            <p className="text-destructive font-medium">Nie udało się załadować wydarzeń.</p>
          ) : events.length === 0 ? (
            <p className="text-muted-foreground">
              {timeframe === EventTimeframe.UPCOMING
                ? "Brak nadchodzących wydarzeń."
                : "Archiwum jest puste."}
            </p>
          ) : (
            <div className="space-y-8">
              <p className="text-muted-foreground text-sm">
                {totalPages > 1
                  ? `Wyświetlono ${firstShown}–${lastShown} z ${totalElements} wydarzeń, strona ${currentPage + 1} z ${totalPages}.`
                  : `Wyświetlono ${totalElements} ${totalElements === 1 ? "wydarzenie" : "wydarzeń"}.`}
              </p>

              <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
                {events.map((item) => {
                  const row = occupancyById.get(item.id);
                  return (
                    <EventCalendarCard
                      key={item.id}
                      event={item}
                      labeled={{ author: item.authorDisplayName }}
                      footer={
                        <div className="mt-auto flex flex-col gap-4 pt-2">
                          {row ? <CardOccupancy row={row} /> : null}
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-muted-foreground w-full text-xs font-semibold tracking-widest uppercase">
                              {audienceLabel(item.audience)}
                            </span>
                            <Button asChild variant="outline" size="sm">
                              <Link href={`${ADMIN_EVENTS_PATH}/${item.slug}`}>
                                <ChartColumn data-icon="inline-start" />
                                Statystyki
                              </Link>
                            </Button>
                            <Button asChild variant="outline" size="sm">
                              <Link href={`${ADMIN_EVENTS_PATH}/${item.slug}/edit`}>
                                <Pencil data-icon="inline-start" />
                                Edytuj
                              </Link>
                            </Button>
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              className="text-destructive hover:text-destructive"
                              onClick={() => setEventToDelete(item)}
                            >
                              <Trash2 data-icon="inline-start" />
                              Usuń
                            </Button>
                          </div>
                        </div>
                      }
                    />
                  );
                })}
              </div>

              {totalPages > 1 ? (
                <div className="flex items-center justify-between gap-4 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage <= 0}
                    onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  >
                    <ArrowLeft data-icon="inline-start" />
                    Poprzednia
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setPage((prev) => prev + 1)}
                  >
                    Następna
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </section>

      <AlertDialog
        open={eventToDelete !== null}
        onOpenChange={(open) => !open && setEventToDelete(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Usunąć wydarzenie?</AlertDialogTitle>
            <AlertDialogDescription>
              „{eventToDelete?.title}” zostanie trwale usunięte. Tej operacji nie można cofnąć.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteEvent.isError ? (
            <p className="text-destructive text-sm font-medium">Nie udało się usunąć wydarzenia.</p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteEvent.isPending}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleteEvent.isPending}
              onClick={(event) => {
                event.preventDefault();
                if (eventToDelete) {
                  deleteEvent.mutate({ id: eventToDelete.id });
                }
              }}
            >
              {deleteEvent.isPending ? "Usuwanie…" : "Usuń wydarzenie"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
