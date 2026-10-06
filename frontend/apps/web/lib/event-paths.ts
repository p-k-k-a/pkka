export type EventPathVariant = "public" | "dashboard";

export function eventDetailHref(id: string, variant: EventPathVariant = "public") {
  return variant === "dashboard" ? `/dashboard/events/${id}` : `/events/${id}`;
}

export function eventsListHref(variant: EventPathVariant = "public", archive = false) {
  const base = variant === "dashboard" ? "/dashboard/events" : "/events";
  return archive ? `${base}/archive` : base;
}
