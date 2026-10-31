"use client";

import Link from "next/link";
import { ADMIN_ALUMNI_DIRECTORY_HREF } from "@/lib/alumni-search-params";
import { format, parseISO } from "date-fns";
import { Download } from "lucide-react";
import {
  EventRegistrationStatus,
  useListEventRegistrations,
  type AdminEventRegistrationResponse,
} from "@pkka/api";
import { formatDateTime, pluralPl } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { downloadCsv, toCsv } from "@/lib/csv";
import { cn } from "@/lib/utils";

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
  const { data: response, isLoading, isError } = useListEventRegistrations(eventId);

  const registrations = response?.data ?? [];
  const seated = registrations.filter(
    (r) => r.status === EventRegistrationStatus.REGISTERED,
  ).length;
  const queued = registrations.length - seated;

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
              {registrations.map((registration) => (
                <tr key={registration.registrationId} className="border-border border-t">
                  <td className="px-6 py-3">
                    {registration.alumnusId ? (
                      <Link
                        href={`${ADMIN_ALUMNI_DIRECTORY_HREF}/${registration.alumnusId}`}
                        className="text-foreground font-semibold underline-offset-4 hover:underline"
                      >
                        {registration.displayName}
                      </Link>
                    ) : (
                      <p className="text-foreground font-semibold">{registration.displayName}</p>
                    )}
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
