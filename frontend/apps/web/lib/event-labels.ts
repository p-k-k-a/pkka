import type { EventDetailsDtoType, EventListItemDtoType } from "@pkka/api";

type EventType = EventListItemDtoType | EventDetailsDtoType;

const EVENT_TYPE_LABELS: Record<EventType, string> = {
  ONLINE: "Online",
  IN_PERSON: "Stacjonarnie",
  HYBRID: "Hybrydowo",
};

export function eventTypeLabel(type?: EventType) {
  return type ? EVENT_TYPE_LABELS[type] : "Wydarzenie";
}

export function eventTypeLabelUpper(type?: EventType) {
  return eventTypeLabel(type).toUpperCase();
}

export function eventLocationLabel(type?: EventType, location?: string) {
  if (location?.trim()) return location;
  if (type === "ONLINE") return "Online";
  return "Miejsce do ustalenia";
}

export function formatSeats(seatLimit?: number, seatsTaken?: number) {
  if (seatLimit == null) return null;
  return `${seatsTaken ?? 0} / ${seatLimit} miejsc`;
}

export function formatSeatsCompact(seatLimit?: number, seatsTaken?: number) {
  if (seatLimit == null) return null;
  return `${seatsTaken ?? 0}/${seatLimit} MIEJSC`;
}

export function formatSeatsRemaining(seatLimit?: number, seatsTaken?: number) {
  if (seatLimit == null) return null;
  return {
    remaining: seatLimit - (seatsTaken ?? 0),
    limit: seatLimit,
  };
}
