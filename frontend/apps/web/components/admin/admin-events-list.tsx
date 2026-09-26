"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, Pencil, Plus, Trash2 } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import {
  getListAdminEventsQueryKey,
  getListEventsQueryKey,
  useDeleteAdminEvent,
  useListAdminEvents,
  EventTimeframe,
  type AdminEventSummaryResponse,
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
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/lib/auth-context";
import { audienceLabel, isAdmin } from "@pkka/domain";

const PAGE_SIZE = 20;
const ADMIN_EVENTS_PATH = "/dashboard/admin/events";

export function AdminEventsList() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const [page, setPage] = useState(0);
  const [timeframe, setTimeframe] = useState<EventTimeframe>(EventTimeframe.ALL);
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
  } = useListAdminEvents({ timeframe, page, size: PAGE_SIZE }, { query: { enabled: admin } });

  const deleteEvent = useDeleteAdminEvent({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: getListAdminEventsQueryKey() });
        queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() });
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

  return (
    <div>
      <section className="bg-background px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
            Kalendarz wydarzeń
          </p>
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            Wydarzenia klubu
          </h1>
          <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
            Twórz, edytuj i usuwaj wydarzenia — także minione i tylko dla alumnów. Karty wyglądają
            tak samo jak w publicznym kalendarzu.
          </p>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <Tabs
              value={timeframe}
              onValueChange={(value) => {
                setTimeframe(value as EventTimeframe);
                setPage(0);
              }}
            >
              <TabsList>
                <TabsTrigger value={EventTimeframe.ALL}>Wszystkie</TabsTrigger>
                <TabsTrigger value={EventTimeframe.UPCOMING}>Nadchodzące</TabsTrigger>
                <TabsTrigger value={EventTimeframe.PAST}>Minione</TabsTrigger>
              </TabsList>
            </Tabs>
            <Button asChild>
              <Link href={`${ADMIN_EVENTS_PATH}/new`}>
                <Plus data-icon="inline-start" />
                Nowe wydarzenie
              </Link>
            </Button>
          </div>
        </div>
      </section>

      <section className="bg-background px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto max-w-[1280px]">
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
              {timeframe === EventTimeframe.ALL
                ? "Brak wydarzeń — utwórz pierwsze."
                : "Brak wydarzeń w tym zakresie."}
            </p>
          ) : (
            <div className="space-y-8">
              <p className="text-muted-foreground text-sm">
                {totalPages > 1
                  ? `Wyświetlono ${firstShown}–${lastShown} z ${totalElements} wydarzeń, strona ${currentPage + 1} z ${totalPages}.`
                  : `Wyświetlono ${totalElements} ${totalElements === 1 ? "wydarzenie" : "wydarzeń"}.`}
              </p>

              <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
                {events.map((item) => (
                  <EventCalendarCard
                    key={item.id}
                    event={item}
                    footer={
                      <div className="mt-auto flex flex-wrap items-center gap-2 pt-2">
                        <span className="text-muted-foreground mr-auto text-xs font-semibold tracking-widest uppercase">
                          {audienceLabel(item.audience)}
                        </span>
                        <Button asChild variant="outline" size="sm">
                          <Link href={`${ADMIN_EVENTS_PATH}/${item.id}`}>
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
                    }
                  />
                ))}
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
