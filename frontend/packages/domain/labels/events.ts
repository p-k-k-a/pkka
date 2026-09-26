import { Audience, EventType } from "@pkka/api";

const EVENT_TYPE_LABELS: Record<EventType, string> = {
  ONLINE: "Online",
  IN_PERSON: "Stacjonarnie",
  HYBRID: "Hybrydowo",
};

const AUDIENCE_LABELS: Record<Audience, string> = {
  PUBLIC: "Publiczne",
  ALL_ALUMNI: "Alumini",
  SPECIFIC_GROUP: "Wybrana grupa",
};

export function eventTypeLabelUpper(type: EventType) {
  return EVENT_TYPE_LABELS[type].toUpperCase();
}

export function audienceLabel(audience: Audience) {
  return AUDIENCE_LABELS[audience];
}

export const EVENT_TYPE_OPTIONS = Object.values(EventType).map((value) => ({
  value,
  label: EVENT_TYPE_LABELS[value],
}));

export const AUDIENCE_OPTIONS = Object.values(Audience)
  .filter((value) => value !== Audience.SPECIFIC_GROUP)
  .map((value) => ({
    value,
    label: AUDIENCE_LABELS[value],
  }));

export function eventLocationLabel(type: EventType, location?: string) {
  if (location?.trim()) return location;
  if (type === EventType.ONLINE) return "Online";
  return "Miejsce do ustalenia";
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
