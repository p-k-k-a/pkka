"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Link2, MapPin } from "lucide-react";
import { EventListItemDto, EventListItemDtoType, useList } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Card, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { formatEventDateLong } from "@/lib/format-event-datetime";
import { eventTypeLabelUpper, formatSeatsCompact } from "@/lib/event-labels";

const DEFAULT_IMAGE = "/hero.png";

function eventImageSrc(imageUrl?: string) {
  return imageUrl?.startsWith("http") ? imageUrl : DEFAULT_IMAGE;
}

function EventsShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-7xl px-6 py-12 md:py-16">
      <div className="mb-10 space-y-3">
        <h1 className="text-foreground text-3xl font-extrabold tracking-tight md:text-4xl">
          Wydarzenia
        </h1>
      </div>
      {children}
    </div>
  );
}

function EventCardSkeleton({ featured = false }: { featured?: boolean }) {
  if (featured) {
    return (
      <Skeleton className="h-64 rounded-[24px] md:col-span-2 md:h-80" />
    );
  }

  return (
    <Card className="bg-muted gap-3 border-0 p-5 shadow-none">
      <Skeleton className="h-3 w-28" />
      <Skeleton className="h-6 w-3/4" />
      <Skeleton className="h-4 w-1/2" />
      <Skeleton className="h-4 w-24" />
      <div className="flex gap-2 pt-1">
        <Skeleton className="h-5 w-24 rounded-full" />
        <Skeleton className="h-5 w-20 rounded-full" />
      </div>
    </Card>
  );
}

function EventCardCompact({ event }: { event: EventListItemDto }) {
  const isOnline = event.type === EventListItemDtoType.ONLINE;
  const LocationIcon = isOnline ? Link2 : MapPin;
  const seats = formatSeatsCompact(event.seatLimit, event.seatsTaken);
  const showLocation = Boolean(event.location?.trim());

  return (
    <Link
      href={`/events/${event.id}`}
      className="group focus-visible:ring-ring flex h-full rounded-3xl focus-visible:ring-2 focus-visible:outline-none"
    >
      <Card className="bg-muted flex h-full w-full flex-col gap-3 border-0 p-5 shadow-none transition-opacity group-hover:opacity-90">
        <div className="space-y-1">
          <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
            {formatEventDateLong(event.startsAt)}
          </p>
          <h2 className="text-foreground text-xl leading-tight font-bold">{event.title}</h2>
        </div>

        {showLocation ? (
          <div className="text-muted-foreground flex items-center gap-2 text-sm">
            <LocationIcon className="size-3.5 shrink-0" aria-hidden="true" />
            <span>{event.location}</span>
          </div>
        ) : null}

        <div className="flex items-center gap-1.5 pt-1">
          <span className="text-foreground text-xs font-bold tracking-widest uppercase">
            Szczegóły
          </span>
          <ArrowRight className="size-3.5" aria-hidden="true" />
        </div>

        <div className="mt-auto flex flex-wrap gap-2">
          <Badge variant="default" className="rounded-full uppercase">
            {eventTypeLabelUpper(event.type)}
          </Badge>
          {seats ? (
            <Badge variant="outline" className="rounded-full uppercase">
              {seats}
            </Badge>
          ) : null}
        </div>
      </Card>
    </Link>
  );
}

function EventCardFeatured({ event }: { event: EventListItemDto }) {
  const isOnline = event.type === EventListItemDtoType.ONLINE;
  const LocationIcon = isOnline ? Link2 : MapPin;
  const seats = formatSeatsCompact(event.seatLimit, event.seatsTaken);
  const showLocation = Boolean(event.location?.trim());

  return (
    <Link
      href={`/events/${event.id}`}
      className="group focus-visible:ring-ring flex rounded-[24px] focus-visible:ring-2 focus-visible:outline-none md:col-span-2"
    >
      <Card className="border-border/70 bg-card text-card-foreground hover:bg-muted/30 flex w-full flex-col overflow-hidden rounded-[24px] border p-0 shadow-sm transition-all duration-300 hover:shadow-md md:flex-row">
        <div className="bg-muted relative aspect-[4/3] w-full shrink-0 overflow-hidden md:aspect-auto md:w-[50%]">
          <Image
            src={eventImageSrc(event.coverImageUrl)}
            alt={event.title ?? "Wydarzenie"}
            fill
            sizes="(max-width: 768px) 100vw, 50vw"
            className="object-cover transition-transform duration-500 group-hover:scale-[1.02]"
            priority
          />
          <div className="absolute top-4 left-4">
            <Badge
              variant="default"
              className="rounded-full uppercase"
            >
              {eventTypeLabelUpper(event.type)}
            </Badge>
          </div>
        </div>

        <div className="flex flex-1 flex-col justify-between p-8 md:p-10">
          <div className="space-y-4">
            <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
              {formatEventDateLong(event.startsAt)}
            </p>

            <CardTitle className="text-foreground group-hover:text-primary text-2xl leading-tight font-extrabold transition-colors md:text-3xl">
              {event.title}
            </CardTitle>

            {showLocation ? (
              <div className="text-muted-foreground flex items-center gap-2 text-sm">
                <LocationIcon className="size-3.5 shrink-0" aria-hidden="true" />
                <span>{event.location}</span>
              </div>
            ) : null}

            {seats ? (
              <Badge variant="outline" className="rounded-full uppercase">
                {seats}
              </Badge>
            ) : null}
          </div>

          <div className="border-border/70 mt-6 flex items-center justify-start border-t pt-6">
            <span className="text-foreground flex items-center gap-1.5 text-xs font-bold tracking-widest uppercase transition-transform group-hover:translate-x-1">
              Szczegóły
              <ArrowRight className="size-3.5" aria-hidden="true" />
            </span>
          </div>
        </div>
      </Card>
    </Link>
  );
}

export function EventsList() {
  const { data: response, isLoading, isError } = useList({ size: 20 });
  const events = response?.data?.content ?? [];

  if (isLoading) {
    return (
      <EventsShell>
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          <EventCardSkeleton featured />
          {Array.from({ length: 3 }).map((_, i) => (
            <EventCardSkeleton key={i} />
          ))}
        </div>
      </EventsShell>
    );
  }

  if (isError) {
    return (
      <EventsShell>
        <p className="text-destructive font-medium">Nie udało się załadować wydarzeń.</p>
      </EventsShell>
    );
  }

  if (events.length === 0) {
    return (
      <EventsShell>
        <p className="text-muted-foreground">Brak nadchodzących wydarzeń.</p>
      </EventsShell>
    );
  }

  return (
    <EventsShell>
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        {events.map((event, index) =>
          index === 0 ? (
            <EventCardFeatured key={event.id} event={event} />
          ) : (
            <EventCardCompact key={event.id} event={event} />
          ),
        )}
      </div>
    </EventsShell>
  );
}
