"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, ArrowRight, Search } from "lucide-react";
import { useListAlumni } from "@pkka/api";
import {
  ALUMNI_SORT_OPTIONS,
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
import { parseAlumniSearchParams, toAlumniSearchString } from "@/lib/alumni-search-params";
import { useDebouncedValue } from "@/lib/use-debounced-value";

export function AlumniDirectory() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  // The URL is read once: afterwards the state drives the URL, not the other way round.
  const [initial] = useState(() => parseAlumniSearchParams(searchParams));
  const [query, setQuery] = useState(initial.query);
  const [filters, setFilters] = useState<AlumniFilters>(initial.filters);
  const [sort, setSort] = useState<AlumniSortOption>(initial.sort);
  const [page, setPage] = useState(initial.page);
  const debouncedQuery = useDebouncedValue(query);
  const resultsRef = useRef<HTMLDivElement>(null);

  // Kept in the URL so the criteria survive a visit to a profile and can be shared.
  const search = toAlumniSearchString({ query: debouncedQuery, filters, sort, page });
  const currentSearch = searchParams.toString();
  useEffect(() => {
    if (search !== (currentSearch ? `?${currentSearch}` : "")) {
      router.replace(`${pathname}${search}`, { scroll: false });
    }
  }, [search, currentSearch, pathname, router]);

  const { data, isPending, isError, isPlaceholderData, refetch } = useListAlumni(
    { ...buildAlumniParams(debouncedQuery, filters, sort), page },
    // Keeps the current results on screen while the next page or filter loads.
    { query: { placeholderData: (previous) => previous } },
  );
  const pageData = data?.status === 200 ? data.data : undefined;
  const alumni = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;

  function goToPage(next: number) {
    setPage(next);
    // Instant on purpose: a smooth scroll gets cut short when the shorter next page
    // replaces the current one mid-animation.
    resultsRef.current?.scrollIntoView({ block: "start" });
  }

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

          <div ref={resultsRef} className="flex scroll-mt-6 flex-col gap-6">
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
                    <AlumniCard key={alumn.id} alumn={alumn} search={search} />
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
        </div>
      </section>
    </div>
  );
}
