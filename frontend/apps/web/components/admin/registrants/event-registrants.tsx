"use client";

import { useDeferredValue, useState } from "react";
import { format, parseISO } from "date-fns";
import { Download, Search } from "lucide-react";
import {
  EventRegistrationStatus,
  useListEventRegistrations,
  type AdminEventRegistrationResponse,
} from "@pkka/api";
import { foldText, formatDateTime, pluralPl } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { downloadCsv, toCsv } from "@/lib/csv";
import { cn } from "@/lib/utils";

function matches(registration: AdminEventRegistrationResponse, query: string) {
  return foldText(`${registration.displayName} ${registration.email}`).includes(foldText(query));
}

function statusLabel(registration: AdminEventRegistrationResponse) {
  return registration.status === EventRegistrationStatus.WAITLISTED
    ? `Lista rezerwowa #${registration.waitlistPosition ?? "?"}`
    : "Zapisany";
}

function exportCsv(eventSlug: string, registrations: AdminEventRegistrationResponse[]) {
  const csv = toCsv(
    ["Imię i nazwisko", "E-mail", "Status", "Pozycja na liście rezerwowej", "Data dołączenia"],
    registrations.map((registration) => [
      registration.displayName,
      registration.email,
      registration.status === EventRegistrationStatus.WAITLISTED ? "Lista rezerwowa" : "Zapisany",
      registration.waitlistPosition,
      format(parseISO(registration.registeredAt), "yyyy-MM-dd HH:mm"),
    ]),
  );
  downloadCsv(`zapisani-${eventSlug}-${format(new Date(), "yyyy-MM-dd")}.csv`, csv);
}

type EventRegistrantsProps = {
  eventId: string;
  eventSlug: string;
  eventTitle: string;
};

export function EventRegistrants({ eventId, eventSlug, eventTitle }: EventRegistrantsProps) {
  const [query, setQuery] = useState("");
  const deferredQuery = useDeferredValue(query.trim());
  const { data: response, isLoading, isError } = useListEventRegistrations(eventId);

  const registrations = response?.data ?? [];
  const seated = registrations.filter(
    (r) => r.status === EventRegistrationStatus.REGISTERED,
  ).length;
  const queued = registrations.length - seated;
  const filtered = registrations.filter((registration) => matches(registration, deferredQuery));

  return (
    <Card className="gap-0 py-0">
      <div className="flex flex-wrap items-end justify-between gap-4 px-6 pt-6 pb-4">
        <div className="space-y-1">
          <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight">
            Zapisani{isLoading || isError ? "" : ` (${seated})`}
          </h2>
          <p className="text-muted-foreground text-sm">
            {isLoading || isError
              ? "Osoby zapisane na wydarzenie i lista rezerwowa."
              : queued > 0
                ? `Oraz ${queued} ${pluralPl(queued, "osoba", "osoby", "osób")} na liście rezerwowej, w kolejności awansu.`
                : "Nikt nie czeka na liście rezerwowej."}
          </p>
        </div>
        <div className="flex w-full flex-wrap gap-3 sm:w-auto">
          <div className="relative min-w-0 flex-1 sm:w-72 sm:flex-none">
            <Search
              className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
              aria-hidden
            />
            <Input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Szukaj po imieniu lub e-mailu"
              aria-label="Szukaj zapisanych po imieniu lub e-mailu"
              className="pl-9"
            />
          </div>
          <Button
            type="button"
            variant="outline"
            disabled={registrations.length === 0}
            onClick={() => exportCsv(eventSlug, registrations)}
          >
            <Download data-icon="inline-start" />
            Eksport CSV
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-2 px-6 pb-6">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-lg" />
          ))}
        </div>
      ) : isError ? (
        <p className="text-destructive px-6 pb-6 font-medium">
          Nie udało się załadować listy zapisanych.
        </p>
      ) : registrations.length === 0 ? (
        <p className="text-muted-foreground px-6 pb-6">Nikt się jeszcze nie zapisał.</p>
      ) : filtered.length === 0 ? (
        <p className="text-muted-foreground px-6 pb-6">Brak wyników dla „{deferredQuery}”.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-left text-sm">
            <caption className="sr-only">Osoby zapisane na wydarzenie „{eventTitle}”</caption>
            <thead className="bg-muted text-muted-foreground text-xs tracking-widest uppercase">
              <tr>
                <th scope="col" className="px-6 py-3 font-semibold">
                  Uczestnik
                </th>
                <th scope="col" className="px-6 py-3 font-semibold">
                  Status
                </th>
                <th scope="col" className="px-6 py-3 text-right font-semibold">
                  Data dołączenia
                </th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((registration) => (
                <tr key={registration.registrationId} className="border-border border-t">
                  <td className="px-6 py-3">
                    <p className="text-foreground font-semibold">{registration.displayName}</p>
                    <p className="text-muted-foreground text-xs break-all">{registration.email}</p>
                  </td>
                  <td className="px-6 py-3">
                    <span
                      className={cn(
                        "inline-flex rounded-lg px-2.5 py-1 text-xs font-semibold",
                        registration.status === EventRegistrationStatus.WAITLISTED
                          ? "bg-primary/15 text-brand-ink dark:text-primary"
                          : "bg-accent/10 text-accent",
                      )}
                    >
                      {statusLabel(registration)}
                    </span>
                  </td>
                  <td className="text-muted-foreground px-6 py-3 text-right whitespace-nowrap tabular-nums">
                    {formatDateTime(registration.registeredAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}
