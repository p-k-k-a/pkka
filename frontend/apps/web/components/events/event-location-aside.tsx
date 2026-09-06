import { Calendar, Clock, MapPin } from "lucide-react";
import {
  eventLocationLabel,
  eventTypeLabelUpper,
  formatEventDateComma,
  formatTimeRange,
} from "@pkka/domain";
import { cn } from "@/lib/utils";

export type EventSchedule = {
  type: string;
  startsAt: string;
  endsAt: string;
  location?: string;
};

function splitLocation(location: string) {
  const trimmed = location.trim();
  const comma = trimmed.indexOf(",");
  if (comma === -1) {
    return { primary: trimmed, secondary: null };
  }
  return {
    primary: trimmed.slice(0, comma).trim(),
    secondary: trimmed.slice(comma + 1).trim() || null,
  };
}

export function EventLocationAside({
  event,
  className,
}: {
  event: EventSchedule;
  className?: string;
}) {
  const location = eventLocationLabel(event.type, event.location);
  const { primary, secondary } = splitLocation(location);

  return (
    <aside className={cn("border-accent border-l-2 pl-6", className)}>
      <p className="text-accent mb-6 text-xs font-semibold tracking-widest uppercase">
        Lokalizacja i czas
      </p>
      <ul className="space-y-5">
        <li className="flex items-start gap-3">
          <Calendar className="text-muted-foreground mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p className="text-foreground text-sm font-semibold">
            {formatEventDateComma(event.startsAt)}
          </p>
        </li>
        <li className="flex items-start gap-3">
          <Clock className="text-muted-foreground mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p className="text-foreground text-sm font-semibold">
            {formatTimeRange(event.startsAt, event.endsAt)}
          </p>
        </li>
        <li className="flex items-start gap-3">
          <MapPin className="text-muted-foreground mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <div className="min-w-0">
            <p className="text-foreground text-sm font-semibold">{primary}</p>
            {secondary ? <p className="text-muted-foreground mt-1 text-sm">{secondary}</p> : null}
          </div>
        </li>
      </ul>
    </aside>
  );
}

export function eventCategoryLabel(type: string) {
  return eventTypeLabelUpper(type);
}
