"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { useListMaterials } from "@pkka/api";
import { SectionTitle } from "@/components/content/section-title";
import { MaterialCard } from "@/components/materials/material-card";
import { eventMaterialsHref } from "@/lib/material-search-params";
import { useVerificationStatus } from "@/lib/use-verification-status";

// Two cards fill the content column; the rest is one click away on the materials page.
const PREVIEW_SIZE = 2;

/** Renders nothing unless the viewer is a verified alumn and the event has materials. */
export function EventMaterialsSection({ eventId }: { eventId: string }) {
  const { isVerified } = useVerificationStatus();
  // The endpoint answers 403 to anyone else, so don't ask.
  const { data } = useListMaterials(
    { eventId, size: PREVIEW_SIZE },
    { query: { enabled: isVerified } },
  );
  const pageData = data?.status === 200 ? data.data : undefined;
  const materials = pageData?.content ?? [];

  if (!isVerified || materials.length === 0) {
    return null;
  }

  const total = pageData?.totalElements ?? materials.length;

  return (
    <section className="mt-16 space-y-6">
      <SectionTitle>Materiały</SectionTitle>
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        {materials.map((material) => (
          // The event's own page doesn't need to name the event again.
          <MaterialCard key={material.id} material={{ ...material, eventTitle: undefined }} />
        ))}
      </div>
      <Link
        href={eventMaterialsHref(eventId)}
        className="text-accent inline-flex items-center gap-1.5 text-sm font-medium hover:underline"
      >
        {total > materials.length ? `Wszystkie materiały (${total})` : "Przejdź do materiałów"}
        <ArrowRight className="size-3.5" aria-hidden="true" />
      </Link>
    </section>
  );
}
