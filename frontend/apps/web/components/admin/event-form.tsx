"use client";

import { useMemo, useState, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import {
  EventRequestAudience,
  EventRequestType,
  getGetAdminEventQueryKey,
  getGetEventByIdQueryKey,
  getListAdminEventsQueryKey,
  getListEventsQueryKey,
  useCreateAdminEvent,
  useListTags,
  useUpdateAdminEvent,
  type AdminEventResponse,
  type EventRequest,
} from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { MarkdownEditor } from "@/components/editor/markdown-editor";
import { AUDIENCE_OPTIONS, EVENT_TYPE_OPTIONS, audienceLabel } from "@/lib/event-labels";
import { fromDatetimeLocalValue, toDatetimeLocalValue } from "@/lib/format-event-datetime";

const ADMIN_EVENTS_PATH = "/dashboard/admin/events";

type EventFormProps = {
  event?: AdminEventResponse;
};

function optionalText(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function parseSeatLimit(value: string) {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number(trimmed);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : undefined;
}

function FieldLabel({ htmlFor, children }: { htmlFor?: string; children: ReactNode }) {
  return (
    <Label
      htmlFor={htmlFor}
      className="text-accent text-xs font-semibold tracking-widest uppercase"
    >
      {children}
    </Label>
  );
}

function SectionTitle({ children }: { children: ReactNode }) {
  return (
    <div className="relative inline-block">
      <span className="bg-muted absolute inset-x-0 bottom-0 h-3" aria-hidden="true" />
      <h2 className="text-accent relative text-xs font-semibold tracking-widest uppercase">
        {children}
      </h2>
    </div>
  );
}

export function EventForm({ event }: EventFormProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isEditing = event !== undefined;

  const [title, setTitle] = useState(event?.title ?? "");
  const [fullDescription, setFullDescription] = useState(event?.fullDescription ?? "");
  const [type, setType] = useState<EventRequest["type"]>(event?.type ?? EventRequestType.ONLINE);
  const [startsAt, setStartsAt] = useState(toDatetimeLocalValue(event?.startsAt));
  const [endsAt, setEndsAt] = useState(toDatetimeLocalValue(event?.endsAt));
  const [registrationClosesAt, setRegistrationClosesAt] = useState(
    toDatetimeLocalValue(event?.registrationClosesAt),
  );
  const [location, setLocation] = useState(event?.location ?? "");
  const [transmissionUrl, setTransmissionUrl] = useState(event?.transmissionUrl ?? "");
  const [seatLimit, setSeatLimit] = useState(
    event?.seatLimit != null ? String(event.seatLimit) : "",
  );
  const [audience, setAudience] = useState<EventRequest["audience"]>(
    event?.audience ?? EventRequestAudience.PUBLIC,
  );
  const [selectedTags, setSelectedTags] = useState<string[]>(event?.tags ?? []);

  const { data: tagsResponse } = useListTags();
  const catalogTags = tagsResponse?.data ?? [];

  const startsAtIso = fromDatetimeLocalValue(startsAt);
  const endsAtIso = fromDatetimeLocalValue(endsAt);
  const registrationClosesAtIso = fromDatetimeLocalValue(registrationClosesAt);
  const periodValid = !startsAtIso || !endsAtIso || startsAtIso < endsAtIso;
  const registrationWindowValid =
    !registrationClosesAtIso || !startsAtIso || registrationClosesAtIso <= startsAtIso;
  const seatLimitInvalid = seatLimit.trim() !== "" && parseSeatLimit(seatLimit) === undefined;
  const audienceOptions = AUDIENCE_OPTIONS.some((option) => option.value === audience)
    ? AUDIENCE_OPTIONS
    : [...AUDIENCE_OPTIONS, { value: audience, label: audienceLabel(audience) }];

  const onSaved = () => {
    queryClient.invalidateQueries({ queryKey: getListAdminEventsQueryKey() });
    queryClient.invalidateQueries({ queryKey: getListEventsQueryKey() });
    if (event) {
      queryClient.invalidateQueries({ queryKey: getGetAdminEventQueryKey(event.id) });
      queryClient.invalidateQueries({ queryKey: getGetEventByIdQueryKey(event.id) });
    }
    router.push(ADMIN_EVENTS_PATH);
  };

  const createEvent = useCreateAdminEvent({ mutation: { onSuccess: onSaved } });
  const updateEvent = useUpdateAdminEvent({ mutation: { onSuccess: onSaved } });

  const isPending = createEvent.isPending || updateEvent.isPending;
  const isError = createEvent.isError || updateEvent.isError;
  const canSave =
    title.trim().length > 0 &&
    Boolean(startsAtIso) &&
    Boolean(endsAtIso) &&
    periodValid &&
    registrationWindowValid &&
    !seatLimitInvalid &&
    !isPending;

  const payload = useMemo<EventRequest>(
    () => ({
      title: title.trim(),
      fullDescription: optionalText(fullDescription),
      type,
      startsAt: startsAtIso ?? "",
      endsAt: endsAtIso ?? "",
      transmissionUrl: optionalText(transmissionUrl),
      location: optionalText(location),
      seatLimit: parseSeatLimit(seatLimit),
      registrationClosesAt: registrationClosesAtIso,
      audience,
      coverImageUrl: event?.coverImageUrl,
      tags: selectedTags,
    }),
    [
      audience,
      event,
      endsAtIso,
      fullDescription,
      location,
      registrationClosesAtIso,
      seatLimit,
      selectedTags,
      startsAtIso,
      title,
      transmissionUrl,
      type,
    ],
  );

  const handleSave = () => {
    if (!canSave) return;
    if (isEditing) {
      updateEvent.mutate({ id: event.id, data: payload });
    } else {
      createEvent.mutate({ data: payload });
    }
  };

  const toggleTag = (name: string) => {
    setSelectedTags((current) =>
      current.includes(name) ? current.filter((tag) => tag !== name) : [...current, name],
    );
  };

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px]">
        <Button asChild variant="ghost" size="sm" className="mb-8 -ml-2">
          <Link href={ADMIN_EVENTS_PATH}>
            <ArrowLeft data-icon="inline-start" />
            Wróć do listy wydarzeń
          </Link>
        </Button>

        <div className="mb-10 flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-3">
            <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
              {isEditing ? "Edycja" : "Nowe wydarzenie"}
            </p>
            <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              {isEditing ? "Edytuj wydarzenie" : "Dodaj wydarzenie do kalendarza"}
            </h1>
          </div>
          <Button
            type="button"
            size="xl"
            className="gap-2"
            disabled={!canSave}
            onClick={handleSave}
          >
            {isPending ? "Zapisywanie…" : isEditing ? "Zapisz zmiany" : "Opublikuj wydarzenie"}
            {isPending ? null : <ArrowRight data-icon="inline-end" />}
          </Button>
        </div>

        {isError ? (
          <p className="text-destructive mb-6 font-medium">
            Nie udało się zapisać wydarzenia. Spróbuj ponownie.
          </p>
        ) : null}

        {!periodValid ? (
          <p className="text-destructive mb-6 font-medium">
            Data zakończenia musi być późniejsza niż data rozpoczęcia.
          </p>
        ) : null}

        {!registrationWindowValid ? (
          <p className="text-destructive mb-6 font-medium">
            Zapisy muszą zamknąć się przed startem wydarzenia.
          </p>
        ) : null}

        {seatLimitInvalid ? (
          <p className="text-destructive mb-6 font-medium">
            Limit miejsc musi być liczbą całkowitą większą lub równą zero.
          </p>
        ) : null}

        <div className="grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-12">
            <section className="space-y-5">
              <FieldLabel htmlFor="event-title">Tytuł</FieldLabel>
              <Input
                id="event-title"
                value={title}
                maxLength={200}
                placeholder="np. Między Zoomem a doświadczeniem…"
                className="font-heading h-auto min-h-14 py-3 text-lg font-semibold md:text-[23px]"
                onChange={(changeEvent) => setTitle(changeEvent.target.value)}
              />
            </section>

            <section className="space-y-5">
              <SectionTitle>O wydarzeniu</SectionTitle>
              <MarkdownEditor
                initialContent={event?.fullDescription ?? ""}
                onChange={setFullDescription}
                ariaLabel="Opis wydarzenia"
              />
            </section>

            <section className="space-y-5">
              <SectionTitle>Termin i miejsce</SectionTitle>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <FieldLabel htmlFor="event-starts-at">Start</FieldLabel>
                  <Input
                    id="event-starts-at"
                    type="datetime-local"
                    value={startsAt}
                    onChange={(changeEvent) => setStartsAt(changeEvent.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <FieldLabel htmlFor="event-ends-at">Koniec</FieldLabel>
                  <Input
                    id="event-ends-at"
                    type="datetime-local"
                    value={endsAt}
                    onChange={(changeEvent) => setEndsAt(changeEvent.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <FieldLabel htmlFor="event-location">Lokalizacja</FieldLabel>
                <Input
                  id="event-location"
                  value={location}
                  maxLength={300}
                  placeholder="Budynek D-17, sala 1.20, Kawiory 21, Kraków"
                  onChange={(changeEvent) => setLocation(changeEvent.target.value)}
                />
                <p className="text-muted-foreground text-xs">
                  Część przed przecinkiem będzie pogrubiona w panelu „Lokalizacja i czas”.
                </p>
              </div>
              <div className="space-y-2">
                <FieldLabel htmlFor="event-transmission-url">Link do transmisji</FieldLabel>
                <Input
                  id="event-transmission-url"
                  value={transmissionUrl}
                  maxLength={500}
                  placeholder="https://"
                  onChange={(changeEvent) => setTransmissionUrl(changeEvent.target.value)}
                />
              </div>
            </section>

            <section className="space-y-4">
              <SectionTitle>Tagi wydarzenia</SectionTitle>
              {catalogTags.length === 0 ? (
                <p className="text-muted-foreground text-sm">Brak tagów w katalogu.</p>
              ) : (
                <div className="flex flex-wrap gap-3">
                  {catalogTags.map((tag) => (
                    <div key={tag.id} className="flex items-center gap-2">
                      <Checkbox
                        id={`event-tag-${tag.id}`}
                        checked={selectedTags.includes(tag.name)}
                        onCheckedChange={() => toggleTag(tag.name)}
                      />
                      <Label htmlFor={`event-tag-${tag.id}`} className="text-sm font-normal">
                        #{tag.name}
                      </Label>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>

          <div className="space-y-10">
            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <SectionTitle>Ustawienia</SectionTitle>
              <div className="space-y-2">
                <FieldLabel htmlFor="event-type">Forma</FieldLabel>
                <Select
                  id="event-type"
                  value={type}
                  onChange={(changeEvent) =>
                    setType(changeEvent.target.value as EventRequest["type"])
                  }
                >
                  {EVENT_TYPE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="space-y-2">
                <FieldLabel htmlFor="event-audience">Odbiorcy</FieldLabel>
                <Select
                  id="event-audience"
                  value={audience}
                  onChange={(changeEvent) =>
                    setAudience(changeEvent.target.value as EventRequest["audience"])
                  }
                >
                  {audienceOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="space-y-2">
                <FieldLabel htmlFor="event-seat-limit">Limit miejsc</FieldLabel>
                <Input
                  id="event-seat-limit"
                  type="number"
                  min={0}
                  inputMode="numeric"
                  value={seatLimit}
                  placeholder="Bez limitu"
                  onChange={(changeEvent) => setSeatLimit(changeEvent.target.value)}
                />
              </div>
              <div className="space-y-2">
                <FieldLabel htmlFor="event-registration-closes-at">Zamknięcie zapisów</FieldLabel>
                <Input
                  id="event-registration-closes-at"
                  type="datetime-local"
                  value={registrationClosesAt}
                  onChange={(changeEvent) => setRegistrationClosesAt(changeEvent.target.value)}
                />
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
