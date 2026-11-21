import { EventType } from "@pkka/api";

export function hasOnlineLink(type: EventType) {
  return type === EventType.ONLINE || type === EventType.HYBRID;
}
