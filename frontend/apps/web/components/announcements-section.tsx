"use client";

import Link from "next/link";
import { useListPosts } from "@pkka/api";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { CardLinkFooter } from "@/components/content/card-link-footer";
import { FeaturedCard } from "@/components/content/featured-card";
import { PublishedMeta } from "@/components/content/published-meta";
import { SectionShell } from "@/components/content/section-shell";
import { DEFAULT_COVER_IMAGE } from "@/lib/content-images";

type AnnouncementsSectionProps = {
  /** "dashboard" makes the news the page itself, with its own heading band. */
  variant?: "public" | "dashboard";
};

const GRID_CLASS = "grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3";
const FEATURED_SPAN_CLASS = "md:col-span-2 lg:col-span-3";

export function AnnouncementsSkeleton() {
  return (
    <div className={GRID_CLASS}>
      <Skeleton className={`bg-muted h-80 w-full rounded-none ${FEATURED_SPAN_CLASS}`} />
      {Array.from({ length: 3 }).map((_, i) => (
        <Skeleton key={i} className="bg-muted h-56 w-full rounded-none" />
      ))}
    </div>
  );
}

function AnnouncementsGrid({ basePath }: { basePath: string }) {
  const { data: response, isLoading, isError } = useListPosts({ size: 10 });
  const announcements = response?.data?.content ?? [];

  if (isLoading) return <AnnouncementsSkeleton />;
  if (isError) {
    return <p className="text-destructive font-medium">Nie udało się załadować aktualności.</p>;
  }
  if (announcements.length === 0) {
    return <p className="text-muted-foreground">Brak opublikowanych wpisów.</p>;
  }

  return (
    <div className={GRID_CLASS}>
      {announcements.map((post, index) => {
        const href = `${basePath}/${post.slug ?? ""}`;

        if (index === 0) {
          return (
            <FeaturedCard
              key={post.id}
              href={href}
              imageSrc={DEFAULT_COVER_IMAGE}
              imageAlt=""
              label="Najnowsze"
              title={post.title ?? ""}
              meta={<PublishedMeta publishedAt={post.publishedAt} />}
              cta="Czytaj dalej"
              className={FEATURED_SPAN_CLASS}
            />
          );
        }

        return (
          <Link
            key={post.id}
            href={href}
            className="group focus-visible:ring-ring block h-full rounded-none transition-opacity hover:opacity-90 focus-visible:ring-2 focus-visible:outline-none"
          >
            <Card className="bg-muted flex h-full flex-col gap-6 overflow-visible rounded-none p-6 shadow-none ring-0">
              <h3 className="font-heading text-foreground text-lg leading-snug font-semibold">
                {post.title}
              </h3>
              <PublishedMeta publishedAt={post.publishedAt} />
              <CardLinkFooter>Czytaj dalej</CardLinkFooter>
            </Card>
          </Link>
        );
      })}
    </div>
  );
}

export function AnnouncementsSection({ variant = "public" }: AnnouncementsSectionProps) {
  if (variant === "public") {
    return (
      <SectionShell title="Aktualności" as="section">
        <AnnouncementsGrid basePath="/announcements" />
      </SectionShell>
    );
  }

  return (
    <div>
      <section className="bg-background px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
            Aktualności
          </p>
          <h1 className="font-heading text-foreground max-w-3xl text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
            Co słychać w Klubie Alumnów
          </h1>
          <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
            Ogłoszenia, relacje i informacje od zespołu Klubu oraz Wydziału Informatyki AGH.
          </p>
        </div>
      </section>

      <section className="bg-background px-4 pb-10 md:px-10 md:pb-20">
        <div className="mx-auto max-w-[1280px]">
          <AnnouncementsGrid basePath="/dashboard/announcements" />
        </div>
      </section>
    </div>
  );
}
