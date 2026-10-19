"use client";

import { Calendar, Clock } from "lucide-react";
import { useGetPost } from "@pkka/api";
import { formatPublishedAt } from "@pkka/domain";
import { CoverImage } from "@/components/content/cover-image";
import { DetailBackLink } from "@/components/content/detail-back-link";
import { ProseContent } from "@/components/content/prose-content";
import { Skeleton } from "@/components/ui/skeleton";

type AnnouncementDetailProps = {
  slug: string;
  backHref?: string;
};

const BACK_LABEL = "Wróć do aktualności";

export function AnnouncementDetail({ slug, backHref = "/" }: AnnouncementDetailProps) {
  const { data: response, isLoading, isError, isFetching } = useGetPost(slug);
  const post = response?.data;

  if (isLoading && !post) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto grid max-w-[1280px] grid-cols-1 gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-6">
            <Skeleton className="h-6 w-32 rounded-lg" />
            <Skeleton className="h-16 w-full max-w-xl rounded-lg" />
            <Skeleton className="aspect-[16/9] w-full rounded-lg" />
            <Skeleton className="h-40 w-full rounded-lg" />
          </div>
          <Skeleton className="h-40 w-full rounded-lg" />
        </div>
      </div>
    );
  }

  if (isError || !post) {
    return (
      <div className="mx-auto max-w-[1280px] px-4 py-16 text-center md:px-10">
        {isError ? (
          <p className="text-destructive mb-6 font-semibold">Nie udało się załadować wpisu.</p>
        ) : (
          <>
            <h1 className="font-heading mb-2 text-2xl font-semibold">Nie znaleziono wpisu</h1>
            <p className="text-muted-foreground mb-8">
              {isFetching ? "Szukamy wpisu…" : "Ten wpis nie istnieje lub link jest nieaktualny."}
            </p>
          </>
        )}
        <DetailBackLink href={backHref} label={BACK_LABEL} />
      </div>
    );
  }

  const { dateLabel, timeLabel } = formatPublishedAt(post.publishedAt);

  return (
    <article className="bg-background px-4 py-10 md:px-10 md:py-20">
      <div className="mx-auto max-w-[1280px]">
        <DetailBackLink href={backHref} label={BACK_LABEL} />

        <div className="mt-10 grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div>
            <span className="bg-band text-band-foreground inline-flex rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
              Aktualność
            </span>
            <h1 className="font-heading text-foreground mt-6 text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
              {post.title}
            </h1>

            <CoverImage alt="" className="mt-10" />

            {post.content ? (
              <ProseContent content={post.content} className="text-muted-foreground max-w-2xl" />
            ) : null}
          </div>

          <aside className="border-accent border-l-2 pl-6 lg:sticky lg:top-24">
            <p className="text-accent mb-6 text-xs font-semibold tracking-widest uppercase">
              Publikacja
            </p>
            <ul className="space-y-5">
              <li className="flex items-start gap-3">
                <Calendar
                  className="text-muted-foreground mt-0.5 size-4 shrink-0"
                  aria-hidden="true"
                />
                <p className="text-foreground text-sm font-semibold">{dateLabel}</p>
              </li>
              <li className="flex items-start gap-3">
                <Clock
                  className="text-muted-foreground mt-0.5 size-4 shrink-0"
                  aria-hidden="true"
                />
                <p className="text-foreground text-sm font-semibold">{timeLabel}</p>
              </li>
            </ul>
          </aside>
        </div>
      </div>
    </article>
  );
}
