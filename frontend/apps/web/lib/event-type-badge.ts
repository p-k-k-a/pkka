import { EventType } from "@pkka/api";

const EVENT_TYPE_BADGE: Record<EventType, string> = {
  IN_PERSON: "bg-[#4cc38a] text-primary-foreground",
  ONLINE: "bg-[#188ef2] text-primary-foreground",
  HYBRID: "bg-primary text-primary-foreground",
};

export function eventTypeBadgeClass(type: EventType) {
  return EVENT_TYPE_BADGE[type];
}
