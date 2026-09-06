import type { ReactNode } from "react";
import Link from "next/link";
import { ArrowRight, Calendar, Clock, MapPin } from "lucide-react";
import { Card } from "@/components/ui/card";
import { eventLocationLabel, formatEventDateComma, formatTimeRange } from "@pkka/domain";
import { eventDetailHref, type EventPathVariant } from "@/lib/event-paths";
import { cn } from "@/lib/utils";
import { eventCategoryLabel, type EventSchedule } from "@/components/events/event-location-aside";

export type EventCalendarCardData = EventSchedule & {
  id: string;
  title: string;
};

type EventCalendarCardProps = {
  event: EventCalendarCardData;
  variant?: EventPathVariant;
  href?: string;
  preview?: boolean;
  footer?: ReactNode;
  className?: string;
};

export function EventCalendarCard({
  event,
  variant = "public",
  href,
  preview = false,
  footer,
  className,
}: EventCalendarCardProps) {
  const location = eventLocationLabel(event.type, event.location);
  const target = href ?? eventDetailHref(event.id, variant);

  const inner = (
    <Card
      className={cn(
        "bg-muted flex h-full flex-col gap-6 overflow-visible rounded-none p-6 shadow-none ring-0",
        className,
      )}
    >
      <span className="bg-band text-band-foreground inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
        {eventCategoryLabel(event.type)}
      </span>
      <h3 className="font-heading text-foreground text-lg leading-snug font-semibold">
        {event.title.trim() || "Tytuł wydarzenia"}
      </h3>
      <ul className="text-muted-foreground space-y-3 text-sm">
        <li className="flex items-start gap-3">
          <Calendar className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <span>{formatEventDateComma(event.startsAt)}</span>
        </li>
        <li className="flex items-start gap-3">
          <Clock className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <span>{formatTimeRange(event.startsAt, event.endsAt)}</span>
        </li>
        <li className="flex items-start gap-3">
          <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <span>{location}</span>
        </li>
      </ul>
      {footer ?? (
        <div className="mt-auto flex justify-end pt-2">
          <span className="bg-background text-accent inline-flex h-[34px] items-center gap-1.5 rounded-none px-2.5 text-sm font-medium">
            Czytaj dalej
            <ArrowRight className="size-3.5" aria-hidden="true" />
          </span>
        </div>
      )}
    </Card>
  );

  if (preview || footer) {
    return inner;
  }

  return (
    <Link
      href={target}
      className="group focus-visible:ring-ring block h-full rounded-none transition-opacity hover:opacity-90 focus-visible:ring-2 focus-visible:outline-none"
    >
      {inner}
    </Link>
  );
}
