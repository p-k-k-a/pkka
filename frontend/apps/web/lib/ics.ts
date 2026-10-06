type IcsEvent = {
  id: string;
  title: string;
  startsAt: string;
  endsAt: string;
  location?: string;
  url: string;
};

// RFC 5545 caps content lines at 75 octets; Polish letters take two bytes in UTF-8,
// so the fold is measured in bytes, never splitting a character.
const MAX_LINE_OCTETS = 75;
const encoder = new TextEncoder();

function foldLine(line: string) {
  const parts: string[] = [];
  let current = "";
  let currentOctets = 0;
  for (const char of line) {
    const octets = encoder.encode(char).length;
    // Continuation lines start with a space, which counts towards their limit.
    const limit = parts.length === 0 ? MAX_LINE_OCTETS : MAX_LINE_OCTETS - 1;
    if (currentOctets + octets > limit) {
      parts.push(current);
      current = "";
      currentOctets = 0;
    }
    current += char;
    currentOctets += octets;
  }
  parts.push(current);
  return parts.join("\r\n ");
}

function escapeText(value: string) {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,")
    .replace(/\r?\n/g, "\\n");
}

function toIcsDate(iso: string) {
  return new Date(iso)
    .toISOString()
    .replace(/[-:]/g, "")
    .replace(/\.\d{3}/, "");
}

export function buildEventIcs(event: IcsEvent, now: Date = new Date()) {
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Klub Alumnow WI AGH//PKKA//PL",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    "BEGIN:VEVENT",
    // Stable per event, so importing the file again updates the entry instead of duplicating it.
    `UID:${event.id}@pkka`,
    `DTSTAMP:${toIcsDate(now.toISOString())}`,
    `DTSTART:${toIcsDate(event.startsAt)}`,
    `DTEND:${toIcsDate(event.endsAt)}`,
    `SUMMARY:${escapeText(event.title)}`,
    ...(event.location?.trim() ? [`LOCATION:${escapeText(event.location.trim())}`] : []),
    `DESCRIPTION:${escapeText(`Szczegóły wydarzenia: ${event.url}`)}`,
    `URL:${event.url}`,
    "END:VEVENT",
    "END:VCALENDAR",
  ];
  return `${lines.map(foldLine).join("\r\n")}\r\n`;
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
