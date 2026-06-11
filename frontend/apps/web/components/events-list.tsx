"use client";

import Link from "next/link";
import { ArrowRight, Link2, MapPin } from "lucide-react";
import { EventListItemDto, EventListItemDtoType, useList } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { FeaturedCard } from "@/components/content/featured-card";
import { SectionShell } from "@/components/content/section-shell";
import { coverImageSrc } from "@/lib/content-images";
import { formatEventDateLong } from "@/lib/format-event-datetime";
import { eventTypeLabelUpper, formatSeatsCompact } from "@/lib/event-labels";

function EventCardSkeleton({ featured = false }: { featured?: boolean }) {
  if (featured) {
    return <Skeleton className="h-64 rounded-[24px] md:col-span-2 md:h-80" />;
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
  const location = event.location?.trim();

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

        {location ? (
          <div className="text-muted-foreground flex items-center gap-2 text-sm">
            <LocationIcon className="size-3.5 shrink-0" aria-hidden="true" />
            <span>{location}</span>
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
  const location = event.location?.trim();

  return (
    <FeaturedCard
      href={`/events/${event.id}`}
      imageSrc={coverImageSrc(event.coverImageUrl)}
      imageAlt={event.title ?? "Wydarzenie"}
      meta={
        <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
          {formatEventDateLong(event.startsAt)}
        </p>
      }
      title={event.title ?? ""}
      cta={
        <>
          Szczegóły
          <ArrowRight className="size-3.5" aria-hidden="true" />
        </>
      }
      ctaAlign="start"
      imageOverlay={
        <div className="absolute top-4 left-4">
          <Badge variant="default" className="rounded-full uppercase">
            {eventTypeLabelUpper(event.type)}
          </Badge>
        </div>
      }
    >
      {location ? (
        <div className="text-muted-foreground flex items-center gap-2 text-sm">
          <LocationIcon className="size-3.5 shrink-0" aria-hidden="true" />
          <span>{location}</span>
        </div>
      ) : null}
      {seats ? (
        <Badge variant="outline" className="rounded-full uppercase">
          {seats}
        </Badge>
      ) : null}
    </FeaturedCard>
  );
}

export function EventsList() {
  const { data: response, isLoading, isError } = useList({ size: 20 });
  const events = response?.data?.content ?? [];

  return (
    <SectionShell title="Wydarzenia">
      {isLoading ? (
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          <EventCardSkeleton featured />
          {Array.from({ length: 3 }).map((_, i) => (
            <EventCardSkeleton key={i} />
          ))}
        </div>
      ) : isError ? (
        <p className="text-destructive font-medium">Nie udało się załadować wydarzeń.</p>
      ) : events.length === 0 ? (
        <p className="text-muted-foreground">Brak nadchodzących wydarzeń.</p>
      ) : (
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          {events.map((event, index) =>
            index === 0 ? (
              <EventCardFeatured key={event.id} event={event} />
            ) : (
              <EventCardCompact key={event.id} event={event} />
            ),
          )}
        </div>
      )}
    </SectionShell>
  );
}
