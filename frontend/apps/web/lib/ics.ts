import { createEvent } from "ics";

type IcsEvent = {
  id: string;
  title: string;
  startsAt: string;
  endsAt: string;
  location?: string;
  url: string;
};

export function buildEventIcs(event: IcsEvent) {
  const { error, value } = createEvent({
    productId: "-//Klub Alumnow WI AGH//PKKA//PL",
    // Stable per event, so importing the file again updates the entry instead of duplicating it.
    uid: `${event.id}@pkka`,
    title: event.title,
    // Epoch milliseconds are an absolute instant, so the file is written in UTC.
    start: Date.parse(event.startsAt),
    end: Date.parse(event.endsAt),
    location: event.location?.trim() || undefined,
    description: `Szczegóły wydarzenia: ${event.url}`,
    url: event.url,
  });
  if (error || !value) {
    throw error ?? new Error("Could not build the calendar file");
  }
  return value;
}

export function icsFileName(title: string) {
  const slug = title
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/ł/g, "l")
    .replace(/Ł/g, "L")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return `${slug || "wydarzenie"}.ics`;
}

export function downloadIcs(fileName: string, content: string) {
  const url = URL.createObjectURL(new Blob([content], { type: "text/calendar;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  // Revoked after a delay: some browsers (Safari, older Firefox) read the blob only after the
  // click handler has returned, so revoking right away can cancel the download.
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
