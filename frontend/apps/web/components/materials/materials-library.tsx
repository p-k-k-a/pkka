"use client";

import { useCallback, useEffect, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, ArrowRight, CalendarDays, X } from "lucide-react";
import { useGetEventById, useListMaterials } from "@pkka/api";
import { MATERIALS_PAGE_SIZE } from "@pkka/domain";
import { MaterialCard } from "@/components/materials/material-card";
import { MaterialSearchInput } from "@/components/materials/material-search-input";
import { MaterialTypeTabs } from "@/components/materials/material-type-tabs";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  parseMaterialsSearchParams,
  toMaterialsSearchString,
  type MaterialsState,
} from "@/lib/material-search-params";

function EventChip({ eventId, onClear }: { eventId: string; onClear: () => void }) {
  const { data } = useGetEventById(eventId);
  const title = data?.status === 200 ? data.data.title : null;

  return (
    <span className="bg-muted text-foreground inline-flex max-w-full items-center gap-2 rounded-lg py-1 pr-1 pl-3 text-sm">
      <CalendarDays className="text-muted-foreground size-4 shrink-0" aria-hidden="true" />
      <span className="truncate">{title ?? "Wybrane wydarzenie"}</span>
      <Button
        type="button"
        variant="ghost"
        size="icon-xs"
        onClick={onClear}
        aria-label="Usuń filtr wydarzenia"
      >
        <X />
      </Button>
    </span>
  );
}

export function MaterialsLibrary() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  // The URL is the only source of truth, so any link to this page (the sidebar, the
  // event page) sets the filters, and they survive a reload or a shared link.
  const { query, type, eventId, page } = parseMaterialsSearchParams(searchParams);
  const resultsRef = useRef<HTMLDivElement>(null);

  const update = useCallback(
    (next: Partial<MaterialsState>) => {
      const search = toMaterialsSearchString({ query, type, eventId, page, ...next });
      router.replace(`${pathname}${search}`, { scroll: false });
    },
    [pathname, router, query, type, eventId, page],
  );

  // Rewrites a hand-edited URL to its canonical form, dropping anything malformed.
  const canonicalSearch = toMaterialsSearchString({ query, type, eventId, page });
  const currentSearch = searchParams.toString();
  useEffect(() => {
    if (canonicalSearch !== (currentSearch ? `?${currentSearch}` : "")) {
      router.replace(`${pathname}${canonicalSearch}`, { scroll: false });
    }
  }, [canonicalSearch, currentSearch, pathname, router]);

  const { data, isPending, isError, isPlaceholderData, refetch } = useListMaterials(
    {
      size: MATERIALS_PAGE_SIZE,
      page,
      ...(query ? { q: query } : {}),
      ...(type ? { type } : {}),
      ...(eventId ? { eventId } : {}),
    },
    // Keeps the current results on screen while the next page or filter loads.
    { query: { placeholderData: (previous) => previous } },
  );
  const pageData = data?.status === 200 ? data.data : undefined;
  const materials = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;
  const filtered = query !== "" || type !== null || eventId !== null;

  // A stale or shared ?page= past the end lands on the last page instead of an empty one.
  const pageOutOfRange = pageData !== undefined && materials.length === 0 && page > 0;
  useEffect(() => {
    if (pageOutOfRange) update({ page: Math.max(0, totalPages - 1) });
  }, [pageOutOfRange, totalPages, update]);

  function goToPage(next: number) {
    update({ page: next });
    resultsRef.current?.scrollIntoView({ block: "start" });
  }

  function clearFilters() {
    update({ query: "", type: null, eventId: null, page: 0 });
  }

  return (
    <div className="flex flex-col">
      <section className="bg-background px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto flex max-w-[1280px] flex-col gap-8">
          <div className="space-y-6">
            <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
              Materiały klubu
            </p>
            <h1 className="font-heading text-foreground max-w-3xl text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
              Nagrania i prezentacje z wydarzeń
            </h1>
            <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
              Wróć do minionych spotkań albo nadrób te, które Cię ominęły. Materiały otwierają się w
              nowej karcie.
            </p>
          </div>

          <MaterialSearchInput
            id="materials-search"
            value={query}
            onCommit={(next) => update({ query: next, page: 0 })}
          />

          <div className="flex flex-wrap items-center gap-3">
            <MaterialTypeTabs value={type} onChange={(next) => update({ type: next, page: 0 })} />
            {eventId ? (
              <EventChip eventId={eventId} onClear={() => update({ eventId: null, page: 0 })} />
            ) : null}
          </div>
        </div>
      </section>

      <section className="bg-background px-4 pb-10 md:px-10 md:pb-20">
        <div ref={resultsRef} className="mx-auto flex max-w-[1280px] scroll-mt-6 flex-col gap-6">
          {isPending || pageOutOfRange ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="bg-muted h-80 w-full rounded-none" />
              ))}
            </div>
          ) : isError && !pageData ? (
            <div className="flex flex-col items-start gap-3">
              <p className="text-destructive font-medium">Nie udało się załadować materiałów.</p>
              <Button type="button" variant="outline" onClick={() => void refetch()}>
                Spróbuj ponownie
              </Button>
            </div>
          ) : materials.length === 0 ? (
            <div className="flex flex-col items-start gap-3">
              <p className="text-muted-foreground">
                {filtered
                  ? "Brak materiałów spełniających kryteria."
                  : "Nie ma jeszcze żadnych materiałów."}
              </p>
              {filtered ? (
                <Button type="button" variant="ghost" className="px-0" onClick={clearFilters}>
                  Wyczyść filtry
                </Button>
              ) : null}
            </div>
          ) : (
            <>
              <p className="text-muted-foreground text-sm" aria-live="polite">
                Znaleziono: <span className="text-foreground font-semibold">{totalElements}</span>
              </p>
              <div
                className={`grid grid-cols-1 gap-8 transition-opacity md:grid-cols-2 lg:grid-cols-3 ${isPlaceholderData ? "opacity-60" : ""}`}
              >
                {materials.map((material) => (
                  <MaterialCard key={material.id} material={material} />
                ))}
              </div>
              {totalPages > 1 ? (
                <div className="flex items-center justify-between gap-4 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={page <= 0 || isPlaceholderData}
                    onClick={() => goToPage(Math.max(0, page - 1))}
                  >
                    <ArrowLeft data-icon="inline-start" />
                    Poprzednia
                  </Button>
                  <span className="text-muted-foreground text-sm">
                    Strona {page + 1} z {totalPages}
                  </span>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1 || isPlaceholderData}
                    onClick={() => goToPage(page + 1)}
                  >
                    Następna
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                </div>
              ) : null}
            </>
          )}
        </div>
      </section>
    </div>
  );
}
