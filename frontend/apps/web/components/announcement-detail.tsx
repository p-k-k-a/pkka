"use client";

import { useGetPost } from "@pkka/api";
import { useEffect } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { CoverImage } from "@/components/content/cover-image";
import { DetailBackLink } from "@/components/content/detail-back-link";
import { DetailHeader } from "@/components/content/detail-header";
import { ProseContent } from "@/components/content/prose-content";
import { formatPublishedAt } from "@/lib/format-published-at";

export function AnnouncementDetail({ slug }: { slug: string }) {
  const { data: response, isLoading, isError, isFetching } = useGetPost(slug);
  const post = response?.data;
  const publication = post ? formatPublishedAt(post.publishedAt) : null;

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [slug]);

  if (isLoading && !post) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10 md:py-14">
        <div className="mb-8 flex h-10 items-center gap-3 border-b">
          <Skeleton className="size-8 rounded-full" />
          <Skeleton className="h-4 w-40" />
        </div>
        <Skeleton className="mb-8 aspect-[16/9] w-full rounded-2xl" />
        <Skeleton className="mb-4 h-6 w-32" />
        <Skeleton className="mb-8 h-12 w-full max-w-2xl" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="text-destructive mb-6 font-semibold">Nie udało się załadować ogłoszenia.</p>
        <DetailBackLink href="/" label="Wróć do aktualności" />
      </div>
    );
  }

  if (!post) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <h1 className="mb-2 text-2xl font-bold">Nie znaleziono ogłoszenia</h1>
        <p className="text-muted-foreground mb-8">
          {isFetching
            ? "Szukamy ogłoszenia…"
            : "To ogłoszenie nie istnieje lub link jest nieaktualny."}
        </p>
        <DetailBackLink href="/" label="Wróć do aktualności" />
      </div>
    );
  }

  return (
    <article className="mx-auto max-w-3xl px-6 py-10 md:py-14">
      <DetailHeader backHref="/" title="Szczegóły wpisu" />

      <CoverImage alt={post.title ?? "Aktualność"} />

      <header className="mb-8 space-y-4">
        <div className="text-muted-foreground flex items-center gap-2 text-sm font-semibold">
          <span>{publication?.dateLabel}</span>
          <span>-</span>
          <span>{publication?.timeLabel}</span>
        </div>

        <h1 className="text-foreground text-2xl font-extrabold tracking-tight md:text-3xl md:leading-tight">
          {post.title}
        </h1>
      </header>

      <div className="border-border/70 mt-8 space-y-6 border-t pt-8">
        {post.content ? <ProseContent content={post.content} /> : null}
      </div>

      <footer className="mt-16 flex justify-center border-t pt-10">
        <DetailBackLink href="/" label="Wróć do aktualności" />
      </footer>
    </article>
  );
}
