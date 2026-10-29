"use client";

import { useState } from "react";
import {
  ArrowRight,
  ArrowUpRight,
  CalendarPlus,
  CircleCheck,
  Download,
  Hourglass,
  Video,
} from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  EventRegistrationStatus,
  getGetEventBySlugQueryKey,
  getListEventsQueryKey,
  useCancelEventRegistration,
  useRegisterForEvent,
  type EventDetailsResponse,
} from "@pkka/api";
import {
  eventCalendarDetails,
  eventRegistrationErrorMessage,
  type EventRegistrationAction,
  formatSeatsRemaining,
  googleCalendarUrl,
  hasEventStarted,
  hasMeeting,
  isEventFull,
  isEventRegistrationClosed,
  joinMeetingLabel,
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
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { eventDetailHref } from "@/lib/event-paths";
import { buildEventIcs, downloadIcs } from "@/lib/ics";

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
  const [calendarOpen, setCalendarOpen] = useState(false);
  // Covers the refetch as well as the request: until fresh data arrives the buttons still
  // show the old state, and a second click would hit 409 or 404.
  const [pending, setPending] = useState(false);

  const register = useRegisterForEvent();
  const unregister = useCancelEventRegistration();

  const status = event.registrationStatus;
  const waitlisted = status === EventRegistrationStatus.WAITLISTED;
  const full = isEventFull(event.seatLimit, event.seatsTaken);
  const closed = isEventRegistrationClosed(event);
  // The backend refuses to sign anyone out once the event has started.
  const started = hasEventStarted(event.startsAt);
  const meeting = hasMeeting(event.type);
  const meetingUrl = event.transmissionUrl;

  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: getGetEventBySlugQueryKey(event.slug) }),
      queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() }),
    ]);

  // Built on click, not during render: the page URL needs `window`.
  const eventPageUrl = () => `${window.location.origin}${eventDetailHref(event.slug)}`;

  const openGoogleCalendar = () => {
    const url = googleCalendarUrl({
      ...event,
      location: event.location?.trim() || meetingUrl,
      details: eventCalendarDetails(eventPageUrl(), meetingUrl),
    });
    setCalendarOpen(false);
    if (url) {
      window.open(url, "_blank", "noopener,noreferrer");
    } else {
      toast.error("Nie udało się otworzyć Kalendarza Google.");
    }
  };

  const downloadCalendarFile = () => {
    setCalendarOpen(false);
    try {
      downloadIcs(`${event.slug}.ics`, buildEventIcs({ ...event, url: eventPageUrl() }));
    } catch {
      toast.error("Nie udało się przygotować pliku kalendarza.");
    }
  };

  // A toast rather than an inline line: when the event has just started, the refetch
  // swaps this view for another one and an inline error would vanish with it.
  const showError = (error: unknown, action: EventRegistrationAction) => {
    const sessionExpired = error instanceof ApiError && error.status === 401;
    toast.error(eventRegistrationErrorMessage(error, action), {
      action: sessionExpired ? { label: "Zaloguj się", onClick: loginWithKeycloak } : undefined,
    });
  };

  const onJoin = async () => {
    setPending(true);
    try {
      const { data: registration } = await register.mutateAsync({ eventId: event.id });
      if (registration.status === EventRegistrationStatus.REGISTERED) {
        toast.success("Zapisano Cię na wydarzenie.");
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
        {meeting && !meetingUrl ? (
          <p className="text-muted-foreground text-xs">
            {waitlisted
              ? "Link do spotkania dostaniesz, gdy zwolni się dla Ciebie miejsce."
              : "Link do spotkania pojawi się tutaj, gdy organizator go doda."}
          </p>
        ) : null}
        <div className="flex flex-wrap gap-3">
          {meetingUrl ? (
            <Button size="xl" className="gap-2" asChild>
              <a href={meetingUrl} target="_blank" rel="noopener noreferrer">
                <Video data-icon="inline-start" />
                {joinMeetingLabel(meetingUrl)}
              </a>
            </Button>
          ) : null}
          {waitlisted || started ? null : (
            <Popover open={calendarOpen} onOpenChange={setCalendarOpen}>
              <PopoverTrigger asChild>
                <Button size="xl" variant={meetingUrl ? "outline" : "default"} className="gap-2">
                  <CalendarPlus data-icon="inline-start" />
                  Dodaj do kalendarza
                </Button>
              </PopoverTrigger>
              <PopoverContent align="start" className="w-64 gap-1 p-1">
                <Button variant="ghost" className="justify-start" onClick={openGoogleCalendar}>
                  <ArrowUpRight data-icon="inline-start" />
                  Kalendarz Google
                </Button>
                <Button variant="ghost" className="justify-start" onClick={downloadCalendarFile}>
                  <Download data-icon="inline-start" />
                  Plik .ics (Apple, Outlook)
                </Button>
              </PopoverContent>
            </Popover>
          )}
          {started ? null : (
            <Button
              size="xl"
              variant="outline"
              disabled={pending}
              onClick={() => setConfirmingLeave(true)}
            >
              {waitlisted ? "Opuść listę rezerwową" : "Wypisz się"}
            </Button>
          )}
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
