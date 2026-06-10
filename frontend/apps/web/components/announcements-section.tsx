"use client";

import Link from "next/link";
import { useListPosts } from "@pkka/api";
import { Card, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { CardLinkFooter } from "@/components/content/card-link-footer";
import { FeaturedCard } from "@/components/content/featured-card";
import { SectionShell } from "@/components/content/section-shell";
import { DEFAULT_COVER_IMAGE } from "@/lib/content-images";
import { formatPublishedAt } from "@/lib/format-published-at";

export function AnnouncementsSection() {
  const { data: response, isLoading, isError } = useListPosts({ size: 10 });
  const announcements = response?.data?.content ?? [];

  if (isLoading) {
    return (
      <SectionShell title="Publiczne Aktualności" as="section">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton
              key={i}
              className={`h-64 rounded-[24px] ${i === 0 ? "md:col-span-2 md:h-80" : ""}`}
            />
          ))}
        </div>
      </SectionShell>
    );
  }

  if (isError) {
    return (
      <SectionShell title="Publiczne Aktualności" as="section">
        <p className="text-destructive font-medium">Nie udało się załadować ogłoszeń.</p>
      </SectionShell>
    );
  }

  if (announcements.length === 0) {
    return (
      <SectionShell title="Publiczne Aktualności" as="section">
        <p className="text-muted-foreground">Brak opublikowanych wpisów.</p>
      </SectionShell>
    );
  }

  return (
    <SectionShell title="Publiczne Aktualności" as="section">
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        {announcements.map((post, index) => {
          const isFeatured = index === 0;
          const { dateLabel, timeLabel } = formatPublishedAt(post.publishedAt);
          const slug = post.slug ?? post.id ?? String(index);

          if (isFeatured) {
            return (
              <FeaturedCard
                key={post.id}
                href={`/announcements/${slug}`}
                imageSrc={DEFAULT_COVER_IMAGE}
                imageAlt={post.title ?? "Aktualność"}
                meta={
                  <div className="text-muted-foreground flex items-center gap-2 text-sm font-semibold">
                    <span>{dateLabel}</span>
                    <span>•</span>
                    <span>{timeLabel}</span>
                  </div>
                }
                title={post.title ?? ""}
                cta="CZYTAJ WIĘCEJ →"
              />
            );
          }

          return (
            <Link
              key={post.id}
              href={`/announcements/${slug}`}
              className="group focus-visible:ring-ring flex h-full rounded-[24px] focus-visible:ring-2 focus-visible:outline-none"
            >
              <Card className="border-border/70 bg-card text-card-foreground hover:bg-muted/30 flex w-full flex-col justify-between rounded-[24px] border p-8 shadow-sm transition-all duration-300 hover:shadow-md">
                <div className="space-y-4">
                  <div className="text-muted-foreground flex items-center gap-2 text-sm font-semibold">
                    <span>{dateLabel}</span>
                    <span>-</span>
                    <span>{timeLabel}</span>
                  </div>

                  <CardTitle className="text-foreground group-hover:text-primary text-xl leading-tight font-bold transition-colors">
                    {post.title}
                  </CardTitle>
                </div>

                <CardLinkFooter className="mt-8 border-t pt-6">CZYTAJ WIĘCEJ →</CardLinkFooter>
              </Card>
            </Link>
          );
        })}
      </div>
    </SectionShell>
  );
}
