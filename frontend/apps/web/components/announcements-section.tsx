"use client";

import Link from "next/link";
import Image from "next/image";
import { useGetAnnouncements } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Card, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

import { Avatar } from "@/components/ui/avatar";

function announcementImageSrc(imageUrl: string) {
  return imageUrl.startsWith("/") || imageUrl.startsWith("http") ? imageUrl : "/hero.png";
}

export function AnnouncementsSection() {
  const { data: response, isLoading, isError } = useGetAnnouncements();
  const announcements = response?.data ?? [];

  if (isLoading) {
    return (
      <section className="mx-auto max-w-7xl px-6 py-12 md:py-20">
        <h2 className="mb-10 text-3xl font-extrabold tracking-tight md:text-4xl text-foreground">
          Publiczne Aktualności
        </h2>
        <div className="grid gap-8 grid-cols-1 md:grid-cols-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className={`h-64 rounded-[24px] ${i === 0 ? "md:col-span-2 md:h-80" : ""}`} />
          ))}
        </div>
      </section>
    );
  }

  if (isError) {
    return (
      <section className="mx-auto max-w-7xl px-6 py-12 md:py-20">
        <p className="text-destructive font-medium">Nie udało się załadować ogłoszeń.</p>
      </section>
    );
  }

  return (
    <section className="mx-auto max-w-7xl px-6 py-12 md:py-20">
      <h2 className="mb-10 text-3xl font-extrabold tracking-tight md:text-4xl text-foreground">
        Publiczne Aktualności
      </h2>

      <div className="grid gap-8 grid-cols-1 md:grid-cols-2">
        {announcements.map((post, index) => {
          const isFeatured = index === 0;

          if (isFeatured) {
            return (
              <Link
                key={post.id}
                href={`/announcements/${post.slug}`}
                className="group md:col-span-2 flex focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-[24px]"
              >
                <Card className="overflow-hidden flex w-full flex-col rounded-[24px] border border-border/70 bg-card p-0 text-card-foreground shadow-sm transition-all duration-300 hover:bg-muted/30 hover:shadow-md md:flex-row">
                  <div className="relative shrink-0 overflow-hidden bg-muted md:w-[50%] w-full aspect-[4/3] md:aspect-auto">
                    <Image
                      src={announcementImageSrc(post.imageUrl)}
                      alt={post.title}
                      fill
                      sizes="(max-width: 768px) 100vw, 50vw"
                      className="object-cover transition-transform duration-500 group-hover:scale-102"
                      priority
                    />
                    <div className="absolute top-4 left-4">
                      <Badge variant="secondary" className="rounded-md border-none bg-background/90 px-2.5 py-1 text-foreground shadow-none hover:bg-background">
                        {post.tag}
                      </Badge>
                    </div>
                  </div>

                  <div className="flex flex-col flex-1 justify-between p-8 md:p-10">
                    <div className="space-y-4">
                      <div className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
                        <span>{post.date}</span>
                        <span>•</span>
                        <span>{post.time}</span>
                      </div>

                      <CardTitle className="text-2xl font-extrabold leading-tight text-foreground transition-colors group-hover:text-primary md:text-3xl">
                        {post.title}
                      </CardTitle>

                      <p className="line-clamp-3 leading-relaxed text-muted-foreground md:line-clamp-4">
                        {post.description}
                      </p>
                    </div>

                    <div className="mt-6 flex items-center justify-between border-t border-border/70 pt-6">
                      <div className="flex items-center gap-3">
                        <Avatar
                          src={post.author?.avatarUrl}
                          fallback={post.author?.name}
                          alt={post.author?.name}
                          size="md"
                        />
                        <div>
                          <div className="text-sm font-semibold text-foreground">
                            {post.author?.name}
                          </div>
                          <div className="text-[10px] font-bold tracking-wider text-muted-foreground uppercase">
                            {post.author?.role}
                          </div>
                        </div>
                      </div>

                      <span className="text-xs font-bold tracking-widest text-foreground uppercase transition-transform group-hover:translate-x-1">
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
              className="group flex focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-[24px] h-full"
            >
              <Card className="flex w-full flex-col justify-between rounded-[24px] border border-border/70 bg-card p-8 text-card-foreground shadow-sm transition-all duration-300 hover:bg-muted/30 hover:shadow-md">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
                      <span>{post.date}</span>
                      <span>-</span>
                      <span>{post.time}</span>
                    </div>
                    <Badge variant="secondary" className="shadow-none rounded-md px-2 py-0.5 text-xs font-semibold">
                      {post.tag}
                    </Badge>
                  </div>

                  <CardTitle className="text-xl font-bold leading-tight text-foreground transition-colors group-hover:text-primary">
                    {post.title}
                  </CardTitle>

                  <p className="line-clamp-3 text-sm leading-relaxed text-muted-foreground">
                    {post.description}
                  </p>
                </div>

                <div className="mt-8 flex flex-col justify-between gap-4 border-t border-border/70 pt-6 sm:flex-row sm:items-center">
                  <div className="flex items-center gap-3">
                    <Avatar
                      src={post.author?.avatarUrl}
                      fallback={post.author?.name}
                      alt={post.author?.name}
                      size="sm"
                    />
                    <div>
                      <div className="text-sm font-semibold text-foreground">
                        {post.author?.name}
                      </div>
                      <div className="text-[10px] font-bold tracking-wider text-muted-foreground uppercase">
                        {post.author?.role}
                      </div>
                    </div>
                  </div>

                  <span className="self-start text-xs font-bold tracking-widest text-foreground uppercase transition-transform group-hover:translate-x-1 sm:self-auto">
                    CZYTAJ WIĘCEJ →
                  </span>
                </div>
              </Card>
            </Link>
          );
        })}
      </div>
    </section>
  );
}

