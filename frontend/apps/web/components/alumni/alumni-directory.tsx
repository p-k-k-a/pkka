"use client";

import { useState } from "react";
import { ArrowLeft, ArrowRight, Search } from "lucide-react";
import { useListAlumni } from "@pkka/api";
import {
  ALUMNI_SORT_OPTIONS,
  DEFAULT_ALUMNI_SORT,
  EMPTY_ALUMNI_FILTERS,
  buildAlumniParams,
  type AlumniFilters,
  type AlumniSortOption,
} from "@pkka/domain";
import { AlumniCard } from "@/components/alumni/alumni-card";
import { AlumniFiltersPanel } from "@/components/alumni/alumni-filters";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useDebouncedValue } from "@/lib/use-debounced-value";

const SEARCH_DEBOUNCE_MS = 300;

export function AlumniDirectory() {
  const [query, setQuery] = useState("");
  const [filters, setFilters] = useState<AlumniFilters>(EMPTY_ALUMNI_FILTERS);
  const [sort, setSort] = useState<AlumniSortOption>(DEFAULT_ALUMNI_SORT);
  const [page, setPage] = useState(0);
  const debouncedQuery = useDebouncedValue(query, SEARCH_DEBOUNCE_MS);

  const { data, isPending, isError, isPlaceholderData, refetch } = useListAlumni(
    { ...buildAlumniParams(debouncedQuery, filters, sort), page },
    // Keeps the current results on screen while the next page or filter loads.
    { query: { placeholderData: (previous) => previous } },
  );
  const pageData = data?.status === 200 ? data.data : undefined;
  const alumni = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;

  // Any change to the criteria starts again from the first page.
  function updateFilters(next: AlumniFilters) {
    setFilters(next);
    setPage(0);
  }

  return (
    <div className="flex flex-col">
      <section className="bg-background px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto flex max-w-[1280px] flex-col gap-8">
          <div className="space-y-6">
            <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
              Katalog alumnów
            </p>
            <h1 className="font-heading text-foreground max-w-3xl text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
              Znajdź absolwentów WI AGH
            </h1>
            <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
              Szukaj po nazwisku, stanowisku lub firmie i odezwij się do osób, które chętnie dzielą
              się doświadczeniem.
            </p>
          </div>

          <div className="flex flex-col gap-3 md:flex-row">
            <div className="relative flex-1">
              <Label htmlFor="alumni-search" className="sr-only">
                Szukaj alumnów
              </Label>
              <Search
                className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
                aria-hidden="true"
              />
              <Input
                id="alumni-search"
                type="search"
                value={query}
                onChange={(event) => {
                  setQuery(event.target.value);
                  setPage(0);
                }}
                placeholder="Szukaj po nazwisku, firmie…"
                className="h-11 pl-9"
              />
            </div>
            <div className="md:w-64">
              <Label htmlFor="alumni-sort" className="sr-only">
                Sortuj
              </Label>
              <Select
                id="alumni-sort"
                value={sort}
                onChange={(event) => {
                  setSort(event.target.value as AlumniSortOption);
                  setPage(0);
                }}
              >
                {ALUMNI_SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-background px-4 pb-10 md:px-10 md:pb-20">
        <div className="mx-auto grid max-w-[1280px] items-start gap-8 lg:grid-cols-[260px_1fr]">
          <aside className="lg:sticky lg:top-6">
            <AlumniFiltersPanel value={filters} onChange={updateFilters} />
          </aside>

          <div className="flex flex-col gap-6">
            {isPending ? (
              <div className="grid grid-cols-1 gap-8 md:grid-cols-2 xl:grid-cols-3">
                {Array.from({ length: 6 }).map((_, i) => (
                  <Skeleton key={i} className="bg-muted h-72 w-full rounded-none" />
                ))}
              </div>
            ) : isError && !pageData ? (
              <div className="flex flex-col items-start gap-3">
                <p className="text-destructive font-medium">Nie udało się załadować alumnów.</p>
                <Button type="button" variant="outline" onClick={() => void refetch()}>
                  Spróbuj ponownie
                </Button>
              </div>
            ) : alumni.length === 0 ? (
              <p className="text-muted-foreground">Brak alumnów spełniających kryteria.</p>
            ) : (
              <>
                <p className="text-muted-foreground text-sm" aria-live="polite">
                  Znaleziono: <span className="text-foreground font-semibold">{totalElements}</span>
                </p>
                <div
                  className={`grid grid-cols-1 gap-8 transition-opacity md:grid-cols-2 xl:grid-cols-3 ${isPlaceholderData ? "opacity-60" : ""}`}
                >
                  {alumni.map((alumn) => (
                    <AlumniCard key={alumn.id} alumn={alumn} />
                  ))}
                </div>
                {totalPages > 1 ? (
                  <div className="flex items-center justify-between gap-4 pt-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={page <= 0 || isPlaceholderData}
                      onClick={() => setPage((prev) => Math.max(0, prev - 1))}
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
                      onClick={() => setPage((prev) => prev + 1)}
                    >
                      Następna
                      <ArrowRight data-icon="inline-end" />
                    </Button>
                  </div>
                ) : null}
              </>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
