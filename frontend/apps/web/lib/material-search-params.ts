import type { MaterialType } from "@pkka/api";
import { isMaterialType } from "@pkka/domain";

export const MATERIALS_HREF = "/dashboard/materials";

export type MaterialsState = {
  type: MaterialType | null;
  eventId: string | null;
  /** Zero-based, like the API; the URL shows it one-based. */
  page: number;
};

type ReadableSearchParams = { get(name: string): string | null };

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// The URL can be edited by hand or shared, so anything malformed falls back to
// the default instead of reaching the API.
export function parseMaterialsSearchParams(params: ReadableSearchParams): MaterialsState {
  const type = params.get("type");
  const eventId = params.get("event");
  const page = Number(params.get("page"));

  return {
    type: isMaterialType(type) ? type : null,
    eventId: eventId && UUID.test(eventId) ? eventId : null,
    page: Number.isInteger(page) && page > 1 ? page - 1 : 0,
  };
}

/** `?…` for the given state, or "" when everything is at its default. */
export function toMaterialsSearchString({ type, eventId, page }: MaterialsState) {
  const params = new URLSearchParams();

  if (type) params.set("type", type);
  if (eventId) params.set("event", eventId);
  if (page > 0) params.set("page", String(page + 1));

  const search = params.toString();
  return search ? `?${search}` : "";
}

export function eventMaterialsHref(eventId: string) {
  return `${MATERIALS_HREF}${toMaterialsSearchString({ type: null, eventId, page: 0 })}`;
}
