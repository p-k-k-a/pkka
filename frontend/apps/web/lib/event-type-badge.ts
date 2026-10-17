import { EventType } from "@pkka/api";

const EVENT_TYPE_BADGE: Record<EventType, string> = {
  IN_PERSON: "bg-band text-band-foreground",
  ONLINE: "bg-accent text-accent-foreground",
  HYBRID: "bg-chart-2 text-white",
};

export function eventTypeBadgeClass(type: EventType) {
  return EVENT_TYPE_BADGE[type];
}
