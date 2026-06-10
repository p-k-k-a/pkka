"use client";

import Link from "next/link";
import Image from "next/image";
import { useList } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Card, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { formatEventDateTime } from "@/lib/format-event-datetime";
import { eventLocationLabel, eventTypeLabel, formatSeats } from "@/lib/event-labels";

const DEFAULT_IMAGE = "/hero.png";

function eventImageSrc(imageUrl?: string) {
  return imageUrl?.startsWith("http") ? imageUrl : DEFAULT_IMAGE;
}

function EventsShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-7xl px-6 py-12 md:py-20">
      <h1 className="text-foreground mb-10 text-3xl font-extrabold tracking-tight md:text-4xl">
        Wydarzenia
      </h1>
      {children}
    </div>
  );
}

export function EventsList() {
  const { data: response, isLoading, isError } = useList({ size: 20 });
  const events = response?.data?.content ?? [];

  if (isLoading) {
    return (
      <EventsShell>
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton
              key={i}
              className={`h-64 rounded-[24px] ${i === 0 ? "md:col-span-2 md:h-80" : ""}`}
            />
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
        {events.map((event, index) => {
          const isFeatured = index === 0;
          const { dateLabel, timeLabel } = formatEventDateTime(event.startsAt);
          const seats = formatSeats(event.seatLimit, event.seatsTaken);

          if (isFeatured) {
            return (
              <Link
                key={event.id}
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
                        variant="secondary"
                        className="bg-background/90 text-foreground hover:bg-background rounded-md border-none px-2.5 py-1 shadow-none"
                      >
                        {eventTypeLabel(event.type)}
                      </Badge>
                    </div>
                  </div>

                  <div className="flex flex-1 flex-col justify-between p-8 md:p-10">
                    <div className="space-y-4">
                      <div className="text-muted-foreground flex flex-wrap items-center gap-2 text-sm font-semibold">
                        <span>{dateLabel}</span>
                        <span>•</span>
                        <span>{timeLabel}</span>
                      </div>

                      <CardTitle className="text-foreground group-hover:text-primary text-2xl leading-tight font-extrabold transition-colors md:text-3xl">
                        {event.title}
                      </CardTitle>

                      <p className="text-muted-foreground line-clamp-2 text-sm leading-relaxed">
                        {eventLocationLabel(event.type, event.location)}
                      </p>
                      {seats ? (
                        <p className="text-muted-foreground text-sm font-medium">{seats}</p>
                      ) : null}
                    </div>

                    <div className="border-border/70 mt-6 flex items-center justify-end border-t pt-6">
                      <span className="text-foreground text-xs font-bold tracking-widest uppercase transition-transform group-hover:translate-x-1">
                        ZOBACZ SZCZEGÓŁY →
                      </span>
                    </div>
                  </div>
                </Card>
              </Link>
            );
          }

          return (
            <Link
              key={event.id}
              href={`/events/${event.id}`}
              className="group focus-visible:ring-ring flex h-full rounded-[24px] focus-visible:ring-2 focus-visible:outline-none"
            >
              <Card className="border-border/70 bg-card text-card-foreground hover:bg-muted/30 flex w-full flex-col justify-between rounded-[24px] border p-8 shadow-sm transition-all duration-300 hover:shadow-md">
                <div className="space-y-4">
                  <div className="flex items-center justify-between gap-3">
                    <div className="text-muted-foreground flex items-center gap-2 text-sm font-semibold">
                      <span>{dateLabel}</span>
                      <span>-</span>
                      <span>{timeLabel}</span>
                    </div>
                    <Badge
                      variant="secondary"
                      className="rounded-md px-2 py-0.5 text-xs font-semibold shadow-none"
                    >
                      {eventTypeLabel(event.type)}
                    </Badge>
                  </div>

                  <CardTitle className="text-foreground group-hover:text-primary text-xl leading-tight font-bold transition-colors">
                    {event.title}
                  </CardTitle>

                  <p className="text-muted-foreground line-clamp-2 text-sm leading-relaxed">
                    {eventLocationLabel(event.type, event.location)}
                  </p>
                  {seats ? (
                    <p className="text-muted-foreground text-sm font-medium">{seats}</p>
                  ) : null}
                </div>

                <div className="border-border/70 mt-8 flex justify-end border-t pt-6">
                  <span className="text-foreground text-xs font-bold tracking-widest uppercase transition-transform group-hover:translate-x-1">
                    ZOBACZ SZCZEGÓŁY →
                  </span>
                </div>
              </Card>
            </Link>
          );
        })}
      </div>
    </EventsShell>
  );
}
