"use client";

import Link from "next/link";
import { ArrowRight, Link2, MapPin } from "lucide-react";
import { EventListItemDtoType, useList } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { formatEventDateLong } from "@/lib/format-event-datetime";
import { eventTypeLabelUpper, formatSeatsCompact } from "@/lib/event-labels";

function EventsShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-3xl px-6 py-12 md:py-16">
      <div className="mb-8 space-y-3">
        <h1 className="text-foreground text-3xl font-extrabold tracking-tight md:text-4xl">
          Wydarzenia
        </h1>
        <p className="text-muted-foreground leading-relaxed">
          Przeglądaj publiczne spotkania i prelekcje organizowane przez społeczność Alumni WI AGH.
          Dołącz do nas i buduj sieć kontaktów.
        </p>
      </div>
      {children}
    </div>
  );
}

function EventCardSkeleton() {
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

export function EventsList() {
  const { data: response, isLoading, isError } = useList({ size: 20 });
  const events = response?.data?.content ?? [];

  if (isLoading) {
    return (
      <EventsShell>
        <div className="flex flex-col gap-5">
          {Array.from({ length: 4 }).map((_, i) => (
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
      <div className="flex flex-col gap-5">
        {events.map((event) => {
          const isOnline = event.type === EventListItemDtoType.ONLINE;
          const LocationIcon = isOnline ? Link2 : MapPin;
          const seats = formatSeatsCompact(event.seatLimit, event.seatsTaken);
          const showLocation = Boolean(event.location?.trim());

          return (
            <Link
              key={event.id}
              href={`/events/${event.id}`}
              className="group focus-visible:ring-ring rounded-3xl focus-visible:ring-2 focus-visible:outline-none"
            >
              <Card className="bg-muted gap-3 border-0 p-5 shadow-none transition-opacity group-hover:opacity-90">
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

                <div className="flex flex-wrap gap-2">
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
        })}
      </div>
    </EventsShell>
  );
}
