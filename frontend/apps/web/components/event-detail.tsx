"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowLeft, LogIn } from "lucide-react";
import { useGetById } from "@pkka/api";
import { useEffect } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { formatEventDateTime, formatEventDateTimeRange } from "@/lib/format-event-datetime";
import { eventLocationLabel, eventTypeLabel, formatSeats } from "@/lib/event-labels";

const DEFAULT_IMAGE = "/hero.png";

function eventImageSrc(imageUrl?: string) {
  return imageUrl?.startsWith("http") ? imageUrl : DEFAULT_IMAGE;
}

function EventDescription({ content }: { content: string }) {
  const paragraphs = content.split(/\n\n+/).filter((p) => p.trim().length > 0);

  if (paragraphs.length === 0) {
    return <p className="text-foreground/90 text-base leading-8">{content}</p>;
  }

  return (
    <div className="space-y-6">
      {paragraphs.map((paragraph, index) => (
        <p key={index} className="text-foreground/90 text-base leading-8">
          {paragraph.trim()}
        </p>
      ))}
    </div>
  );
}

function HeaderBar() {
  return (
    <div className="border-border/70 mb-8 flex items-center justify-between border-b pb-4">
      <Button
        asChild
        variant="ghost"
        size="icon"
        className="text-muted-foreground hover:text-foreground rounded-full"
      >
        <Link href="/events">
          <ArrowLeft className="size-5" />
          <span className="sr-only">Wróć</span>
        </Link>
      </Button>
      <h2 className="text-foreground text-sm font-bold tracking-widest uppercase">
        SZCZEGÓŁY WYDARZENIA
      </h2>
      <div className="w-10" aria-hidden="true" />
    </div>
  );
}

function BackLink({ className }: { className?: string }) {
  return (
    <Button asChild variant="outline" className={cn("rounded-xl font-semibold", className)}>
      <Link href="/events">
        <ArrowLeft className="mr-2 size-4" />
        Wróć do wydarzeń
      </Link>
    </Button>
  );
}

export function EventDetail({ id }: { id: string }) {
  const { data: response, isLoading, isError, isFetching } = useGetById(id);
  const event = response?.data;
  const seats = event ? formatSeats(event.seatLimit, event.seatsTaken) : null;
  const registrationCloses = event ? formatEventDateTime(event.registrationClosesAt) : null;
  const loginHref = `/login?redirect=${encodeURIComponent(`/events/${id}`)}`;

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [id]);

  if (isLoading && !event) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10 md:py-14">
        <div className="mb-8 flex h-10 items-center justify-between border-b">
          <Skeleton className="size-8 rounded-full" />
          <Skeleton className="h-4 w-32" />
          <div className="w-8" />
        </div>
        <Skeleton className="mb-8 aspect-[16/9] w-full rounded-2xl" />
        <Skeleton className="mb-4 h-6 w-32" />
        <Skeleton className="mb-8 h-12 w-full max-w-2xl" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="text-destructive mb-6 font-semibold">Nie udało się załadować wydarzenia.</p>
        <BackLink />
      </div>
    );
  }

  if (!event) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <h1 className="mb-2 text-2xl font-bold">Nie znaleziono wydarzenia</h1>
        <p className="text-muted-foreground mb-8">
          {isFetching
            ? "Szukamy wydarzenia…"
            : "To wydarzenie nie istnieje lub nie jest już dostępne."}
        </p>
        <BackLink />
      </div>
    );
  }

  return (
    <article className="mx-auto max-w-3xl px-6 py-10 md:py-14">
      <HeaderBar />

      <div className="bg-muted relative mb-8 aspect-[16/9] overflow-hidden rounded-2xl border shadow-sm">
        <Image
          src={eventImageSrc(event.coverImageUrl)}
          alt={event.title ?? "Wydarzenie"}
          fill
          priority
          sizes="(max-width: 768px) 100vw, 768px"
          className="object-cover"
        />
      </div>

      <header className="mb-8 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-muted-foreground text-sm font-semibold">
            {formatEventDateTimeRange(event.startsAt, event.endsAt)}
          </p>
          <Badge
            variant="secondary"
            className="rounded-md px-2.5 py-0.5 text-xs font-semibold shadow-none"
          >
            {eventTypeLabel(event.type)}
          </Badge>
        </div>

        <h1 className="text-foreground text-2xl font-extrabold tracking-tight md:text-3xl md:leading-tight">
          {event.title}
        </h1>

        <div className="text-muted-foreground space-y-1 text-sm">
          <p>{eventLocationLabel(event.type, event.location)}</p>
          {seats ? <p>{seats}</p> : null}
          {event.registrationClosesAt ? (
            <p>
              Zapisy do: {registrationCloses?.dateLabel}, {registrationCloses?.timeLabel}
            </p>
          ) : null}
        </div>

        {event.tags && event.tags.length > 0 ? (
          <div className="flex flex-wrap gap-2 pt-2">
            {event.tags.map((tag) => (
              <Badge key={tag} variant="outline" className="rounded-md capitalize">
                {tag}
              </Badge>
            ))}
          </div>
        ) : null}
      </header>

      <div className="border-border/70 mt-8 space-y-6 border-t pt-8">
        {event.fullDescription ? <EventDescription content={event.fullDescription} /> : null}
      </div>

      <footer className="mt-16 space-y-6 border-t pt-10">
        <div className="flex flex-col items-center gap-4">
          <Button asChild size="lg" className="rounded-xl px-8 font-semibold">
            <Link href={loginHref}>
              <LogIn className="mr-2 size-4" />
              Zaloguj się, aby dołączyć
            </Link>
          </Button>
          <p className="text-muted-foreground max-w-md text-center text-sm">
            Rejestracja na wydarzenie wymaga zalogowania.
          </p>
        </div>
        <div className="flex justify-center">
          <BackLink />
        </div>
      </footer>
    </article>
  );
}
