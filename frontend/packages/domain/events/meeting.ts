import { EventType } from "@pkka/api";

// The backend accepts meeting links from these hosts only, so the label is known for every
// link it hands out; anything else falls back to a generic button text.
const MEETING_PROVIDERS: { label: string; hosts: RegExp }[] = [
  { label: "Teams", hosts: /(^|\.)teams\.(microsoft|live)\.com$/ },
  { label: "Zoom", hosts: /(^|\.)zoom\.(us|com)$/ },
  { label: "Google Meet", hosts: /^meet\.google\.com$/ },
];

export function meetingProviderLabel(url: string): string | null {
  let host: string;
  try {
    host = new URL(url).hostname.toLowerCase();
  } catch {
    return null;
  }
  return MEETING_PROVIDERS.find((provider) => provider.hosts.test(host))?.label ?? null;
}

export function joinMeetingLabel(url: string) {
  const provider = meetingProviderLabel(url);
  return provider ? `Dołącz przez ${provider}` : "Dołącz do spotkania";
}

export function hasMeeting(type: EventType) {
  return type === EventType.ONLINE || type === EventType.HYBRID;
}
