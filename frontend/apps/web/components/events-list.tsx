"use client";

import { useState } from "react";
import { ArrowLeft, ArrowRight, Check } from "lucide-react";
import { Audience, EventTimeframe, useListEvents } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EventCalendarCard } from "@/components/events/event-calendar-card";
import { EventsArchiveBand } from "@/components/events/events-archive-band";
import { eventsListHref, type EventPathVariant } from "@/lib/event-paths";
import { useVerificationStatus } from "@/lib/use-verification-status";
import { cn } from "@/lib/utils";
import Link from "next/link";

const PAGE_SIZE = 20;

type EventsListProps = {
  variant?: EventPathVariant;
  archive?: boolean;
};

const AUDIENCE_CHIPS = [
  { alumniOnly: false, label: "Wszystkie" },
  { alumniOnly: true, label: "Dla alumnów" },
];

function AudienceChips({ value, onChange }: { value: boolean; onChange: (next: boolean) => void }) {
  return (
    <div role="radiogroup" aria-label="Rodzaj wydarzeń" className="flex flex-wrap gap-2">
      {AUDIENCE_CHIPS.map((chip) => {
        const selected = chip.alumniOnly === value;
        return (
          <button
            key={chip.label}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(chip.alumniOnly)}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full border px-4 py-2 text-sm font-semibold transition-colors",
              selected
                ? "bg-band-foreground border-band-foreground text-band"
                : "border-border text-foreground hover:bg-muted",
            )}
          >
            {selected ? <Check className="size-4" aria-hidden="true" /> : null}
            {chip.label}
          </button>
        );
      })}
    </div>
  );
}

function EventCardSkeleton() {
  return <Skeleton className="bg-muted h-72 w-full rounded-none" />;
}

export function EventsList({ variant = "public", archive = false }: EventsListProps) {
  const [page, setPage] = useState(0);
  const [alumniOnly, setAlumniOnly] = useState(false);
  const { isVerified } = useVerificationStatus();
  const timeframe = archive ? EventTimeframe.PAST : EventTimeframe.UPCOMING;
  const {
    data: response,
    isLoading,
    isError,
  } = useListEvents({
    page,
    size: PAGE_SIZE,
    timeframe,
    ...(isVerified && alumniOnly ? { audience: Audience.ALL_ALUMNI } : {}),
    ...(archive ? { sort: ["startsAt,desc"] } : {}),
  });
  const pageData = response?.data;
  const events = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;

  return (
    <div className="flex min-h-full flex-col">
      <section
        className={`${variant === "dashboard" ? "bg-background" : "bg-muted"} px-4 py-10 md:px-10 md:py-20`}
      >
        <div className="mx-auto max-w-[1280px] space-y-6">
          <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
            {archive ? "Archiwum" : "Kalendarz wydarzeń"}
          </p>
          <h1 className="font-heading text-foreground max-w-3xl text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
            {archive
              ? "Wydarzenia, które już się odbyły"
              : "Nie przegap tego, co dzieje się w Klubie"}
          </h1>
          {archive ? (
            <Button asChild variant="link" size="lg" className="text-accent h-auto px-0 underline">
              <Link href={eventsListHref(variant)}>
                Wróć do nadchodzących wydarzeń
                <ArrowRight data-icon="inline-end" />
              </Link>
            </Button>
          ) : (
            <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
              Znajdziesz tutaj informacje o nadchodzących spotkaniach, szkoleniach, turniejach i
              innych aktywnościach, a także formularze zapisów na najbliższe wydarzenia.
            </p>
          )}
        </div>
      </section>

      <section id="kalendarz" className="bg-background flex-1 px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px] space-y-8">
          {isVerified ? (
            <AudienceChips
              value={alumniOnly}
              onChange={(next) => {
                setAlumniOnly(next);
                setPage(0);
              }}
            />
          ) : null}
          {isLoading ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <EventCardSkeleton key={i} />
              ))}
            </div>
          ) : isError ? (
            <p className="text-destructive font-medium">Nie udało się załadować wydarzeń.</p>
          ) : events.length === 0 ? (
            <p className="text-muted-foreground">
              {archive ? "Brak wydarzeń w archiwum." : "Brak nadchodzących wydarzeń."}
            </p>
          ) : (
            <div className="space-y-8">
              <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
                {events.map((event) => (
                  <EventCalendarCard key={event.id} event={event} variant={variant} />
                ))}
              </div>
              {totalPages > 1 ? (
                <div className="flex items-center justify-between gap-4 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage <= 0}
                    onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  >
                    <ArrowLeft data-icon="inline-start" />
                    Poprzednia
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setPage((prev) => prev + 1)}
                  >
                    Następna
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </section>

      {archive ? null : <EventsArchiveBand variant={variant} />}
    </div>
  );
}
