const EVENT_TYPE_LABELS = {
  ONLINE: "Online",
  IN_PERSON: "Stacjonarnie",
  HYBRID: "Hybrydowo",
} as const;

type EventType = keyof typeof EVENT_TYPE_LABELS;

const AUDIENCE_LABELS = {
  PUBLIC: "Publiczne",
  ALL_ALUMNI: "Alumini",
  SPECIFIC_GROUP: "Wybrana grupa",
} as const;

type Audience = keyof typeof AUDIENCE_LABELS;

export function eventTypeLabel(type?: string) {
  return type && type in EVENT_TYPE_LABELS ? EVENT_TYPE_LABELS[type as EventType] : "Wydarzenie";
}

export function eventTypeLabelUpper(type?: string) {
  return eventTypeLabel(type).toUpperCase();
}

export function audienceLabel(audience?: string) {
  return audience && audience in AUDIENCE_LABELS
    ? AUDIENCE_LABELS[audience as Audience]
    : "Odbiorcy";
}

export const EVENT_TYPE_OPTIONS = (Object.keys(EVENT_TYPE_LABELS) as EventType[]).map((value) => ({
  value,
  label: EVENT_TYPE_LABELS[value],
}));

export const AUDIENCE_OPTIONS = (Object.keys(AUDIENCE_LABELS) as Audience[])
  .filter((value) => value !== "SPECIFIC_GROUP")
  .map((value) => ({
    value,
    label: AUDIENCE_LABELS[value],
  }));

export function eventLocationLabel(type?: string, location?: string) {
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
