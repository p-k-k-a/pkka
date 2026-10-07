"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import {
  EventTimeframe,
  MaterialType,
  getGetAdminMaterialQueryKey,
  getListAdminMaterialsQueryKey,
  getListMaterialsQueryKey,
  useCreateAdminMaterial,
  useListAdminEvents,
  useUpdateAdminMaterial,
  type AdminMaterialResponse,
  type MaterialRequest,
} from "@pkka/api";
import {
  MATERIAL_TITLE_MAX,
  MATERIAL_TYPE_OPTIONS,
  MATERIAL_URL_MAX,
  formatEventDateShort,
  isMaterialType,
  isValidMaterialUrl,
} from "@pkka/domain";
import { FieldLabel } from "@/components/admin/field-label";
import { SectionTitle } from "@/components/content/section-title";
import { MaterialCard } from "@/components/materials/material-card";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

export const ADMIN_MATERIALS_PATH = "/dashboard/admin/materials";

// Enough for the club's whole calendar in one select; events are added a few a month.
const EVENT_OPTIONS_SIZE = 200;

type MaterialFormProps = {
  material?: AdminMaterialResponse;
};

export function MaterialForm({ material }: MaterialFormProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isEditing = material !== undefined;

  const [title, setTitle] = useState(material?.title ?? "");
  const [description, setDescription] = useState(material?.description ?? "");
  const [type, setType] = useState<MaterialType>(material?.type ?? MaterialType.RECORDING);
  const [url, setUrl] = useState(material?.url ?? "");
  const [eventId, setEventId] = useState(material?.eventId ?? "");

  const eventsQuery = useListAdminEvents({
    timeframe: EventTimeframe.ALL,
    size: EVENT_OPTIONS_SIZE,
    // Materials mostly follow recent events, so those come first.
    sort: ["startsAt,desc"],
  });
  const events = eventsQuery.data?.status === 200 ? (eventsQuery.data.data.content ?? []) : [];
  // A material can point at an event the list no longer returns (e.g. one deleted
  // since); keep it selectable so saving doesn't silently unlink it.
  const eventOptions =
    material?.eventId && !events.some((item) => item.id === material.eventId)
      ? [{ id: material.eventId, title: material.eventTitle ?? "Wydarzenie", startsAt: undefined }]
      : [];
  const selectedEventTitle =
    events.find((item) => item.id === eventId)?.title ??
    (eventId === material?.eventId ? material?.eventTitle : undefined);

  const urlInvalid = url.trim() !== "" && !isValidMaterialUrl(url);

  const onSaved = () => {
    queryClient.invalidateQueries({ queryKey: getListAdminMaterialsQueryKey() });
    queryClient.invalidateQueries({ queryKey: getListMaterialsQueryKey() });
    if (material) {
      queryClient.invalidateQueries({ queryKey: getGetAdminMaterialQueryKey(material.id) });
    }
    router.push(ADMIN_MATERIALS_PATH);
  };

  const createMaterial = useCreateAdminMaterial({ mutation: { onSuccess: onSaved } });
  const updateMaterial = useUpdateAdminMaterial({ mutation: { onSuccess: onSaved } });

  const isPending = createMaterial.isPending || updateMaterial.isPending;
  const isError = createMaterial.isError || updateMaterial.isError;
  const canSave = title.trim().length > 0 && isValidMaterialUrl(url) && !isPending;

  const handleSave = () => {
    if (!canSave) return;
    const trimmedDescription = description.trim();
    const payload: MaterialRequest = {
      title: title.trim(),
      description: trimmedDescription || undefined,
      type,
      url: url.trim(),
      eventId: eventId || undefined,
    };
    if (isEditing) {
      updateMaterial.mutate({ id: material.id, data: payload });
    } else {
      createMaterial.mutate({ data: payload });
    }
  };

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px]">
        <Button asChild variant="ghost" size="sm" className="mb-8 -ml-2">
          <Link href={ADMIN_MATERIALS_PATH}>
            <ArrowLeft data-icon="inline-start" />
            Wróć do listy materiałów
          </Link>
        </Button>

        <div className="mb-10 flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-3">
            <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
              {isEditing ? "Edycja" : "Nowy materiał"}
            </p>
            <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              {isEditing ? "Edytuj materiał" : "Dodaj materiał dla alumnów"}
            </h1>
          </div>
          <Button
            type="button"
            size="xl"
            className="gap-2"
            disabled={!canSave}
            onClick={handleSave}
          >
            {isPending ? "Zapisywanie…" : isEditing ? "Zapisz zmiany" : "Dodaj materiał"}
            {isPending ? null : <ArrowRight data-icon="inline-end" />}
          </Button>
        </div>

        {isError ? (
          <p className="text-destructive mb-6 font-medium">
            Nie udało się zapisać materiału. Spróbuj ponownie.
          </p>
        ) : null}

        <div className="grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_360px]">
          <div className="space-y-12">
            <section className="space-y-5">
              <FieldLabel htmlFor="material-title">Tytuł</FieldLabel>
              <Input
                id="material-title"
                value={title}
                maxLength={MATERIAL_TITLE_MAX}
                placeholder="np. Nagranie: Między Zoomem a doświadczeniem"
                className="font-heading h-auto min-h-14 py-3 text-lg font-semibold md:text-[23px]"
                onChange={(changeEvent) => setTitle(changeEvent.target.value)}
              />
            </section>

            <section className="space-y-5">
              <SectionTitle>Link</SectionTitle>
              <div className="space-y-2">
                <FieldLabel htmlFor="material-url">Adres materiału</FieldLabel>
                <Input
                  id="material-url"
                  type="url"
                  value={url}
                  maxLength={MATERIAL_URL_MAX}
                  placeholder="https://www.youtube.com/watch?v=…"
                  aria-invalid={urlInvalid}
                  onChange={(changeEvent) => setUrl(changeEvent.target.value)}
                />
                {urlInvalid ? (
                  <p className="text-destructive text-xs font-medium">
                    Podaj pełny adres zaczynający się od http:// lub https://.
                  </p>
                ) : (
                  <p className="text-muted-foreground text-xs">
                    YouTube, Google Drive lub inny publiczny link. Dla YouTube miniatura pojawi się
                    automatycznie.
                  </p>
                )}
              </div>
            </section>

            <section className="space-y-5">
              <SectionTitle>Opis</SectionTitle>
              <Textarea
                id="material-description"
                aria-label="Opis materiału"
                value={description}
                rows={5}
                placeholder="Krótko: czego dotyczy materiał i dla kogo jest."
                onChange={(changeEvent) => setDescription(changeEvent.target.value)}
              />
            </section>
          </div>

          <div className="space-y-10 lg:sticky lg:top-6">
            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <SectionTitle>Ustawienia</SectionTitle>
              <div className="space-y-2">
                <FieldLabel htmlFor="material-type">Typ</FieldLabel>
                <Select
                  id="material-type"
                  value={type}
                  onChange={(changeEvent) => {
                    if (isMaterialType(changeEvent.target.value)) setType(changeEvent.target.value);
                  }}
                >
                  {MATERIAL_TYPE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="space-y-2">
                <FieldLabel htmlFor="material-event">Wydarzenie</FieldLabel>
                <Select
                  id="material-event"
                  value={eventId}
                  disabled={eventsQuery.isPending}
                  onChange={(changeEvent) => setEventId(changeEvent.target.value)}
                >
                  <option value="">— bez wydarzenia —</option>
                  {[...eventOptions, ...events].map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.startsAt
                        ? `${formatEventDateShort(item.startsAt)} · ${item.title}`
                        : item.title}
                    </option>
                  ))}
                </Select>
                {eventsQuery.isError ? (
                  <p className="text-destructive text-xs font-medium">
                    Nie udało się załadować wydarzeń.
                  </p>
                ) : (
                  <p className="text-muted-foreground text-xs">
                    Materiał zobaczą tylko osoby, które widzą to wydarzenie.
                  </p>
                )}
              </div>
            </Card>

            <div className="space-y-4">
              <SectionTitle>Podgląd karty</SectionTitle>
              <MaterialCard
                material={{
                  title,
                  description: description.trim() || undefined,
                  type,
                  url,
                  eventTitle: selectedEventTitle,
                }}
                footer={<span />}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
