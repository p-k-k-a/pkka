"use client";

import { Calendar, Link2, MapPin, Users } from "lucide-react";
import { EventDetailsDtoType, useGetById } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { CoverImage } from "@/components/content/cover-image";
import { DetailBackLink } from "@/components/content/detail-back-link";
import { DetailHeader } from "@/components/content/detail-header";
import { InfoRow } from "@/components/content/info-row";
import { remoteCoverImageSrc } from "@/lib/content-images";
import { formatEventDateShort, formatTimeRange } from "@/lib/format-event-datetime";
import { eventTypeLabelUpper, formatSeatsRemaining } from "@/lib/event-labels";
import { useAuth } from "@/lib/auth-context";

export function EventDetail({ id }: { id: string }) {
  const { loginWithKeycloak } = useAuth();
  const { data: response, isLoading, isError, isFetching } = useGetById(id);
  const event = response?.data;
  const seats = event ? formatSeatsRemaining(event.seatLimit, event.seatsTaken) : null;
  const coverSrc = event ? remoteCoverImageSrc(event.coverImageUrl) : null;
  const isOnline = event?.type === EventDetailsDtoType.ONLINE;
  const location = event?.location?.trim();

  if (isLoading && !event) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-8">
        <div className="mb-6 flex h-10 items-center gap-3 border-b">
          <Skeleton className="size-8 rounded-full" />
          <Skeleton className="h-4 w-40" />
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

  if (isError || !event) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        {isError ? (
          <p className="text-destructive mb-6 font-semibold">Nie udało się załadować wydarzenia.</p>
        ) : (
          <>
            <h1 className="mb-2 text-2xl font-bold">Nie znaleziono wydarzenia</h1>
            <p className="text-muted-foreground mb-8">
              {isFetching
                ? "Szukamy wydarzenia…"
                : "To wydarzenie nie istnieje lub nie jest już dostępne."}
            </p>
          </>
        )}
        <DetailBackLink href="/events" label="Wróć do wydarzeń" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <article className="px-6 py-8">
        <DetailHeader backHref="/events" title="Szczegóły wydarzenia" />

        <CoverImage
          src={coverSrc}
          alt={event.title ?? "Wydarzenie"}
          aspect="photo"
          showPlaceholder
        />

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
          ) : location ? (
            <InfoRow
              icon={<MapPin className="size-[18px]" />}
              label="Lokalizacja"
              value={location}
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

        <div className="border-border bg-border mb-6 h-px" />

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
        <Button size="xl" className="w-full rounded-xl font-semibold" onClick={loginWithKeycloak}>
          Zaloguj się, aby dołączyć
        </Button>
      </footer>
    </div>
  );
}
