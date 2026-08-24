"use client";

import { ArrowRight } from "lucide-react";
import { useGetEventById } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { DetailBackLink } from "@/components/content/detail-back-link";
import { EventLocationAside, eventCategoryLabel } from "@/components/events/event-location-aside";
import { ProseContent } from "@/components/content/prose-content";
import { eventsListHref, type EventPathVariant } from "@/lib/event-paths";
import { isEventPast } from "@/lib/format-event-datetime";
import { useAuth } from "@/lib/auth-context";

type EventDetailProps = {
  id: string;
  variant?: EventPathVariant;
};

export function EventDetail({ id, variant = "public" }: EventDetailProps) {
  const { isAuthenticated, loginWithKeycloak } = useAuth();
  const { data: response, isLoading, isError, isFetching } = useGetEventById(id);
  const event = response?.data;
  const archived = isEventPast(event?.startsAt);
  const eventsBackHref = eventsListHref(variant, archived);

  if (isLoading && !event) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto grid max-w-[1280px] grid-cols-1 gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-6">
            <Skeleton className="h-6 w-32 rounded-lg" />
            <Skeleton className="h-16 w-full max-w-xl rounded-lg" />
            <Skeleton className="h-24 w-full rounded-lg" />
            <Skeleton className="h-12 w-40 rounded-lg" />
          </div>
          <Skeleton className="h-64 w-full rounded-lg" />
        </div>
      </div>
    );
  }

  if (isError || !event) {
    return (
      <div className="mx-auto max-w-[1280px] px-4 py-16 text-center md:px-10">
        {isError ? (
          <p className="text-destructive mb-6 font-semibold">Nie udało się załadować wydarzenia.</p>
        ) : (
          <>
            <h1 className="font-heading mb-2 text-2xl font-semibold">Nie znaleziono wydarzenia</h1>
            <p className="text-muted-foreground mb-8">
              {isFetching
                ? "Szukamy wydarzenia…"
                : "To wydarzenie nie istnieje lub nie jest już dostępne."}
            </p>
          </>
        )}
        <DetailBackLink href={eventsListHref(variant)} label="Wróć do wydarzeń" />
      </div>
    );
  }

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-20">
      <div className="mx-auto max-w-[1280px]">
        <DetailBackLink
          href={eventsBackHref}
          label={archived ? "Wróć do archiwum" : "Wróć do kalendarza"}
        />

        <div className="mt-10 grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div>
            <span className="bg-navy text-white-text mb-6 inline-flex rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
              {eventCategoryLabel(event.type)}
            </span>
            <h1 className="font-heading text-foreground mt-6 text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
              {event.title}
            </h1>
            {event.shortDescription ? (
              <p className="text-muted-foreground mt-6 max-w-2xl text-base leading-relaxed">
                {event.shortDescription}
              </p>
            ) : null}
            {archived ? null : (
              <div className="mt-8">
                {isAuthenticated ? (
                  <div className="space-y-2">
                    <Button size="xl" className="gap-2" disabled>
                      Zapisz się
                      <ArrowRight data-icon="inline-end" />
                    </Button>
                    <p className="text-muted-foreground text-xs">
                      Rejestracja na wydarzenie wkrótce dostępna.
                    </p>
                  </div>
                ) : (
                  <Button size="xl" className="gap-2" onClick={loginWithKeycloak}>
                    Zapisz się
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                )}
              </div>
            )}

            {event.fullDescription || (event.tags && event.tags.length > 0) ? (
              <section className="mt-16 space-y-6">
                {event.fullDescription ? (
                  <>
                    <div className="relative inline-block">
                      <span
                        className="bg-muted absolute inset-x-0 bottom-0 h-3"
                        aria-hidden="true"
                      />
                      <h2 className="text-accent relative text-xs font-semibold tracking-widest uppercase">
                        O wydarzeniu
                      </h2>
                    </div>
                    <ProseContent
                      content={event.fullDescription}
                      className="text-muted-foreground max-w-2xl"
                    />
                  </>
                ) : null}
                {event.tags && event.tags.length > 0 ? (
                  <div className="flex flex-wrap gap-x-3 gap-y-1">
                    {event.tags.map((tag) => (
                      <span key={tag} className="text-muted-foreground text-sm">
                        #{tag}
                      </span>
                    ))}
                  </div>
                ) : null}
              </section>
            ) : null}
          </div>

          <EventLocationAside event={event} className="lg:sticky lg:top-24" />
        </div>
      </div>
    </div>
  );
}
