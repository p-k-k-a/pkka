"use client";

import { useGetCurrentLegalDocument, type LegalDocumentType } from "@pkka/api";
import { formatPublishedAt } from "@pkka/domain";
import { ProseContent } from "@/components/content/prose-content";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export function LegalDocumentView({ type }: { type: LegalDocumentType }) {
  const {
    data: response,
    isPending,
    isError,
    isFetching,
    refetch,
  } = useGetCurrentLegalDocument(type);
  const document = response?.data;

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-20">
      <article className="mx-auto max-w-3xl">
        {isPending ? (
          <div className="space-y-6">
            <Skeleton className="h-12 w-full max-w-xl rounded-lg" />
            <Skeleton className="h-5 w-48 rounded-lg" />
            <Skeleton className="h-96 w-full rounded-lg" />
          </div>
        ) : isError || !document ? (
          <div className="space-y-4">
            <p className="text-destructive font-semibold">Nie udało się wczytać dokumentu.</p>
            <Button variant="outline" disabled={isFetching} onClick={() => void refetch()}>
              {isFetching ? "Ładowanie…" : "Spróbuj ponownie"}
            </Button>
          </div>
        ) : (
          <>
            <h1 className="font-heading text-foreground text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
              {document.title}
            </h1>
            <p className="text-muted-foreground mt-4 text-sm">
              Wersja {document.version} · obowiązuje od{" "}
              {formatPublishedAt(document.publishedAt).dateLabel}
            </p>
            <ProseContent content={document.content} className="text-muted-foreground mt-10" />
          </>
        )}
      </article>
    </div>
  );
}
