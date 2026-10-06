"use client";

import { useState } from "react";
import { ArrowRight, CalendarPlus, CircleCheck, Hourglass } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  EventRegistrationStatus,
  getGetEventByIdQueryKey,
  getListEventsQueryKey,
  useCancelEventRegistration,
  useRegisterForEvent,
  type EventDetailsResponse,
} from "@pkka/api";
import {
  eventRegistrationConflictMessage,
  formatSeatsRemaining,
  isEventFull,
  isEventRegistrationClosed,
} from "@pkka/domain";
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
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { eventDetailHref } from "@/lib/event-paths";
import { buildEventIcs, downloadIcs, icsFileName } from "@/lib/ics";

type Action = "join" | "leave";

function errorMessage(error: unknown, action: Action) {
  if (error instanceof ApiError) {
    if (error.status === 401) return "Sesja wygasła. Zaloguj się ponownie.";
    const conflict = eventRegistrationConflictMessage(
      (error.body as { reason?: unknown } | null)?.reason,
    );
    if (conflict) return conflict;
    if (error.status === 404) {
      return action === "leave"
        ? "Nie jesteś zapisany na to wydarzenie."
        : "Nie znaleziono wydarzenia.";
    }
  }
  return action === "join"
    ? "Nie udało się zapisać na wydarzenie. Spróbuj ponownie."
    : "Nie udało się wypisać z wydarzenia. Spróbuj ponownie.";
}

function seatsHint(event: EventDetailsResponse) {
  const seats = formatSeatsRemaining(event.seatLimit, event.seatsTaken);
  if (!seats) return null;
  if (seats.remaining <= 0) return "Brak wolnych miejsc — trafisz na listę rezerwową.";
  return `Wolne miejsca: ${seats.remaining} z ${seats.limit}.`;
}

export function EventRegistration({ event }: { event: EventDetailsResponse }) {
  const { isAuthenticated, isLoading: isAuthLoading, loginWithKeycloak } = useAuth();
  const queryClient = useQueryClient();
  const [confirmingLeave, setConfirmingLeave] = useState(false);
  // Covers the refetch as well as the request: until fresh data arrives the buttons still
  // show the old state, and a second click would hit 409 or 404.
  const [pending, setPending] = useState(false);

  const register = useRegisterForEvent();
  const unregister = useCancelEventRegistration();

  const status = event.registrationStatus;
  const waitlisted = status === EventRegistrationStatus.WAITLISTED;
  const full = isEventFull(event.seatLimit, event.seatsTaken);
  const closed = isEventRegistrationClosed(event);

  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: getGetEventByIdQueryKey(event.id) }),
      queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() }),
    ]);

  const addToCalendar = () => {
    const url = `${window.location.origin}${eventDetailHref(event.id)}`;
    downloadIcs(icsFileName(event.title), buildEventIcs({ ...event, url }));
  };

  // A toast rather than an inline line: when the event has just started, the refetch
  // unmounts this component and an inline error would vanish with it.
  const showError = (error: unknown, action: Action) => {
    const sessionExpired = error instanceof ApiError && error.status === 401;
    toast.error(errorMessage(error, action), {
      action: sessionExpired ? { label: "Zaloguj się", onClick: loginWithKeycloak } : undefined,
    });
  };

  const onJoin = async () => {
    setPending(true);
    try {
      const { data: registration } = await register.mutateAsync({ eventId: event.id });
      if (registration.status === EventRegistrationStatus.REGISTERED) {
        toast.success("Zapisano Cię na wydarzenie.", {
          action: { label: "Dodaj do kalendarza", onClick: addToCalendar },
        });
      } else {
        toast.success("Jesteś na liście rezerwowej. Dostaniesz miejsce, gdy ktoś zrezygnuje.");
      }
    } catch (e) {
      showError(e, "join");
    } finally {
      await refresh();
      setPending(false);
    }
  };

  const onLeave = async () => {
    setPending(true);
    try {
      await unregister.mutateAsync({ eventId: event.id });
      toast.success(waitlisted ? "Opuszczono listę rezerwową." : "Wypisano Cię z wydarzenia.");
    } catch (e) {
      showError(e, "leave");
    } finally {
      setConfirmingLeave(false);
      await refresh();
      setPending(false);
    }
  };

  if (isAuthLoading) {
    return <Skeleton className="h-[46px] w-44 rounded-lg" />;
  }

  if (!isAuthenticated) {
    return (
      <Button size="xl" className="gap-2" onClick={loginWithKeycloak}>
        Zapisz się
        <ArrowRight data-icon="inline-end" />
      </Button>
    );
  }

  if (status) {
    return (
      <div className="space-y-4">
        <p className="text-foreground flex items-center gap-2 text-sm font-semibold">
          {waitlisted ? (
            <Hourglass className="text-accent size-4 shrink-0" aria-hidden="true" />
          ) : (
            <CircleCheck className="text-accent size-4 shrink-0" aria-hidden="true" />
          )}
          {waitlisted
            ? "Jesteś na liście rezerwowej. Dostaniesz miejsce, gdy ktoś zrezygnuje."
            : "Jesteś zapisany na to wydarzenie."}
        </p>
        <div className="flex flex-wrap gap-3">
          {waitlisted ? null : (
            <Button size="xl" className="gap-2" onClick={addToCalendar}>
              <CalendarPlus data-icon="inline-start" />
              Dodaj do kalendarza
            </Button>
          )}
          <Button
            size="xl"
            variant="outline"
            disabled={pending}
            onClick={() => setConfirmingLeave(true)}
          >
            {waitlisted ? "Opuść listę rezerwową" : "Wypisz się"}
          </Button>
        </div>

        <AlertDialog
          open={confirmingLeave}
          onOpenChange={(open) => !open && !pending && setConfirmingLeave(false)}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>
                {waitlisted ? "Opuścić listę rezerwową?" : "Wypisać się z wydarzenia?"}
              </AlertDialogTitle>
              <AlertDialogDescription>
                {waitlisted
                  ? "Stracisz swoje miejsce w kolejce."
                  : "Twoje miejsce zostanie zwolnione i może je zająć ktoś z listy rezerwowej."}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={pending}>Anuluj</AlertDialogCancel>
              <AlertDialogAction
                variant="destructive"
                disabled={pending}
                onClick={(clickEvent) => {
                  // Keeps the dialog open until the request settles, so a failure is not lost.
                  clickEvent.preventDefault();
                  void onLeave();
                }}
              >
                {pending ? "Wypisywanie…" : waitlisted ? "Opuść listę rezerwową" : "Wypisz się"}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    );
  }

  if (closed) {
    return (
      <div className="space-y-2">
        <Button size="xl" disabled>
          Zapisy zamknięte
        </Button>
        <p className="text-muted-foreground text-xs">
          Rejestracja na to wydarzenie została zamknięta.
        </p>
      </div>
    );
  }

  const hint = seatsHint(event);
  return (
    <div className="space-y-2">
      <Button size="xl" className="gap-2" disabled={pending} onClick={() => void onJoin()}>
        {pending ? "Zapisywanie…" : full ? "Zapisz się na listę rezerwową" : "Zapisz się"}
        {pending ? null : <ArrowRight data-icon="inline-end" />}
      </Button>
      {hint ? <p className="text-muted-foreground text-xs">{hint}</p> : null}
    </div>
  );
}
