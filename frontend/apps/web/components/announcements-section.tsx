"use client";

import Link from "next/link";
import Image from "next/image";
import { useListPosts } from "@pkka/api";
import { Card, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { formatPublishedAt } from "@/lib/format-published-at";

const DEFAULT_IMAGE = "/hero.png";

function AnnouncementsShell({ children }: { children: React.ReactNode }) {
  return (
    <section className="mx-auto max-w-7xl px-6 py-12 md:py-20">
      <h2 className="text-foreground mb-10 text-3xl font-extrabold tracking-tight md:text-4xl">
        Publiczne Aktualności
      </h2>
      {children}
    </section>
  );
}

export function AnnouncementsSection() {
  const { data: response, isLoading, isError } = useListPosts({ size: 10 });
  const announcements = response?.data?.content ?? [];

  if (isLoading) {
    return (
      <AnnouncementsShell>
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton
              key={i}
              className={`h-64 rounded-[24px] ${i === 0 ? "md:col-span-2 md:h-80" : ""}`}
            />
          ))}
        </div>
      </AnnouncementsShell>
    );
  }

  if (isError) {
    return (
      <AnnouncementsShell>
        <p className="text-destructive font-medium">Nie udało się załadować ogłoszeń.</p>
      </AnnouncementsShell>
    );
  }

  if (announcements.length === 0) {
    return (
      <AnnouncementsShell>
        <p className="text-muted-foreground">Brak opublikowanych wpisów.</p>
      </AnnouncementsShell>
    );
  }

  return (
    <AnnouncementsShell>
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        {announcements.map((post, index) => {
          const isFeatured = index === 0;
          const { dateLabel, timeLabel } = formatPublishedAt(post.publishedAt);

          if (isFeatured) {
            return (
              <Link
                key={post.id}
                href={`/announcements/${post.slug}`}
                className="group focus-visible:ring-ring flex rounded-[24px] focus-visible:ring-2 focus-visible:outline-none md:col-span-2"
              >
                <Card className="border-border/70 bg-card text-card-foreground hover:bg-muted/30 flex w-full flex-col overflow-hidden rounded-[24px] border p-0 shadow-sm transition-all duration-300 hover:shadow-md md:flex-row">
                  <div className="bg-muted relative aspect-[4/3] w-full shrink-0 overflow-hidden md:aspect-auto md:w-[50%]">
                    <Image
                      src={DEFAULT_IMAGE}
                      alt={post.title ?? "Aktualność"}
                      fill
                      sizes="(max-width: 768px) 100vw, 50vw"
                      className="object-cover transition-transform duration-500 group-hover:scale-102"
                      priority
                    />
                  </div>

                  <div className="flex flex-1 flex-col justify-between p-8 md:p-10">
                    <div className="space-y-4">
                      <div className="text-muted-foreground flex items-center gap-2 text-sm font-semibold">
                        <span>{dateLabel}</span>
                        <span>•</span>
                        <span>{timeLabel}</span>
                      </div>

                      <CardTitle className="text-foreground group-hover:text-primary text-2xl leading-tight font-extrabold transition-colors md:text-3xl">
                        {post.title}
                      </CardTitle>
                    </div>

                    <div className="border-border/70 mt-6 flex items-center justify-end border-t pt-6">
                      <span className="text-foreground text-xs font-bold tracking-widest uppercase transition-transform group-hover:translate-x-1">
                        CZYTAJ WIĘCEJ →
                      </span>
                    </div>
                  </div>
                </Card>
              </Link>
            );
          }

          return (
            <Link
              key={post.id}
              href={`/announcements/${post.slug}`}
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

                <div className="border-border/70 mt-8 flex justify-end border-t pt-6">
                  <span className="text-foreground text-xs font-bold tracking-widest uppercase transition-transform group-hover:translate-x-1">
                    CZYTAJ WIĘCEJ →
                  </span>
                </div>
              </Card>
            </Link>
          );
        })}
      </div>
    </AnnouncementsShell>
  );
}
