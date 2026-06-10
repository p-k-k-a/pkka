"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowLeft, Calendar, ImageIcon, Link2, MapPin, Users } from "lucide-react";
import { EventDetailsDtoType, useGetById } from "@pkka/api";
import { useEffect } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import {
  formatEventDateShort,
  formatTimeRange,
} from "@/lib/format-event-datetime";
import {
  eventTypeLabelUpper,
  formatSeatsRemaining,
} from "@/lib/event-labels";

function eventImageSrc(imageUrl?: string) {
  return imageUrl?.startsWith("http") ? imageUrl : null;
}

function InfoRow({
  icon,
  label,
  value,
  sub,
}: {
  icon: React.ReactNode;
  label?: string;
  value: string;
  sub?: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <div className="bg-muted flex size-10 shrink-0 items-center justify-center rounded-md">
        {icon}
      </div>
      <div className="min-w-0 space-y-0.5">
        {label ? (
          <p className="text-muted-foreground text-[10px] font-semibold tracking-widest uppercase">
            {label}
          </p>
        ) : null}
        <p className="text-foreground text-sm font-bold">{value}</p>
        {sub ? <p className="text-muted-foreground text-xs">{sub}</p> : null}
      </div>
    </div>
  );
}

function HeaderBar() {
  return (
    <div className="border-border/70 mb-6 flex items-center justify-between border-b pb-4">
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
        Szczegóły wydarzenia
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
  const seats = event ? formatSeatsRemaining(event.seatLimit, event.seatsTaken) : null;
  const coverSrc = event ? eventImageSrc(event.coverImageUrl) : null;
  const isOnline = event?.type === EventDetailsDtoType.ONLINE;

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [id]);

  if (isLoading && !event) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-8">
        <div className="mb-6 flex h-10 items-center justify-between border-b">
          <Skeleton className="size-8 rounded-full" />
          <Skeleton className="h-4 w-32" />
          <div className="w-8" />
        </div>
        <Skeleton className="mb-6 aspect-[4/3] w-full rounded-xl" />
        <Skeleton className="mb-4 h-5 w-20 rounded-full" />
        <Skeleton className="mb-6 h-10 w-full max-w-xl" />
        <div className="space-y-4">
          <Skeleton className="h-12 w-full rounded-md" />
          <Skeleton className="h-12 w-full rounded-md" />
        </div>
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
    <div className="mx-auto max-w-3xl">
      <article className="px-6 py-8">
        <HeaderBar />

        <div className="border-border bg-muted relative mb-6 flex aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-xl border">
          {coverSrc ? (
            <Image
              src={coverSrc}
              alt={event.title ?? "Wydarzenie"}
              fill
              priority
              sizes="(max-width: 768px) 100vw, 768px"
              className="object-cover"
            />
          ) : (
            <ImageIcon className="text-border size-14" aria-hidden="true" />
          )}
        </div>

        <div className="mb-6 space-y-3">
          <Badge variant="default" className="rounded-full uppercase">
            {eventTypeLabelUpper(event.type)}
          </Badge>
          <h1 className="text-foreground text-3xl leading-tight font-extrabold tracking-tight">
            {event.title}
          </h1>
          {event.tags && event.tags.length > 0 ? (
            <div className="flex flex-wrap gap-x-3 gap-y-1">
              {event.tags.map((tag) => (
                <span key={tag} className="text-muted-foreground text-sm">
                  #{tag}
                </span>
              ))}
            </div>
          ) : null}
        </div>

        <div className="mb-6 space-y-4">
          <InfoRow
            icon={<Calendar className="size-[18px]" />}
            value={formatEventDateShort(event.startsAt)}
            sub={formatTimeRange(event.startsAt, event.endsAt)}
          />

          {isOnline ? (
            <InfoRow
              icon={<Link2 className="size-[18px]" />}
              value="Link do spotkania"
              sub="Link dostępny po zalogowaniu"
            />
          ) : event.location ? (
            <InfoRow
              icon={<MapPin className="size-[18px]" />}
              label="Lokalizacja"
              value={event.location}
            />
          ) : null}

          {seats ? (
            <InfoRow
              icon={<Users className="size-[18px]" />}
              value={`Pozostało ${seats.remaining} miejsc`}
              sub={`Limit: ${seats.limit} osób`}
            />
          ) : null}
        </div>

        <div className="border-border mb-6 h-px bg-border" />

        {event.fullDescription ? (
          <section className="space-y-3">
            <h2 className="text-foreground text-xs font-bold tracking-widest uppercase">
              O wydarzeniu
            </h2>
            <p className="text-muted-foreground leading-7 whitespace-pre-line">
              {event.fullDescription}
            </p>
          </section>
        ) : null}
      </article>

      <footer className="border-border bg-background space-y-3 border-t px-6 py-4">
        <p className="text-muted-foreground text-center text-[10px] font-semibold tracking-widest uppercase">
          Niezalogowani użytkownicy nie mogą dołączyć do wydarzenia
        </p>
        <Button asChild size="xl" className="w-full rounded-xl font-semibold">
          <Link href="/login">Zaloguj się, aby dołączyć</Link>
        </Button>
      </footer>
    </div>
  );
}
