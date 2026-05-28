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
        <h2 className="text-foreground mb-10 text-3xl font-extrabold tracking-tight md:text-4xl">
          Publiczne Aktualności
        </h2>
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton
              key={i}
              className={`h-64 rounded-[24px] ${i === 0 ? "md:col-span-2 md:h-80" : ""}`}
            />
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
      <h2 className="text-foreground mb-10 text-3xl font-extrabold tracking-tight md:text-4xl">
        Publiczne Aktualności
      </h2>

      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        {announcements.map((post, index) => {
          const isFeatured = index === 0;

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
                      src={announcementImageSrc(post.imageUrl)}
                      alt={post.title}
                      fill
                      sizes="(max-width: 768px) 100vw, 50vw"
                      className="object-cover transition-transform duration-500 group-hover:scale-102"
                      priority
                    />
                    <div className="absolute top-4 left-4">
                      <Badge
                        variant="secondary"
                        className="bg-background/90 text-foreground hover:bg-background rounded-md border-none px-2.5 py-1 shadow-none"
                      >
                        {post.tag}
                      </Badge>
                    </div>
                  </div>

                  <div className="flex flex-1 flex-col justify-between p-8 md:p-10">
                    <div className="space-y-4">
                      <div className="text-muted-foreground flex items-center gap-2 text-sm font-semibold">
                        <span>{post.date}</span>
                        <span>•</span>
                        <span>{post.time}</span>
                      </div>

                      <CardTitle className="text-foreground group-hover:text-primary text-2xl leading-tight font-extrabold transition-colors md:text-3xl">
                        {post.title}
                      </CardTitle>

                      <p className="text-muted-foreground line-clamp-3 leading-relaxed md:line-clamp-4">
                        {post.description}
                      </p>
                    </div>

                    <div className="border-border/70 mt-6 flex items-center justify-between border-t pt-6">
                      <div className="flex items-center gap-3">
                        <Avatar
                          src={post.author?.avatarUrl}
                          fallback={post.author?.name}
                          alt={post.author?.name}
                          size="md"
                        />
                        <div>
                          <div className="text-foreground text-sm font-semibold">
                            {post.author?.name}
                          </div>
                          <div className="text-muted-foreground text-[10px] font-bold tracking-wider uppercase">
                            {post.author?.role}
                          </div>
                        </div>
                      </div>

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
                  <div className="flex items-center justify-between">
                    <div className="text-muted-foreground flex items-center gap-2 text-sm font-semibold">
                      <span>{post.date}</span>
                      <span>-</span>
                      <span>{post.time}</span>
                    </div>
                    <Badge
                      variant="secondary"
                      className="rounded-md px-2 py-0.5 text-xs font-semibold shadow-none"
                    >
                      {post.tag}
                    </Badge>
                  </div>

                  <CardTitle className="text-foreground group-hover:text-primary text-xl leading-tight font-bold transition-colors">
                    {post.title}
                  </CardTitle>

                  <p className="text-muted-foreground line-clamp-3 text-sm leading-relaxed">
                    {post.description}
                  </p>
                </div>

                <div className="border-border/70 mt-8 flex flex-col justify-between gap-4 border-t pt-6 sm:flex-row sm:items-center">
                  <div className="flex items-center gap-3">
                    <Avatar
                      src={post.author?.avatarUrl}
                      fallback={post.author?.name}
                      alt={post.author?.name}
                      size="sm"
                    />
                    <div>
                      <div className="text-foreground text-sm font-semibold">
                        {post.author?.name}
                      </div>
                      <div className="text-muted-foreground text-[10px] font-bold tracking-wider uppercase">
                        {post.author?.role}
                      </div>
                    </div>
                  </div>

                  <span className="text-foreground self-start text-xs font-bold tracking-widest uppercase transition-transform group-hover:translate-x-1 sm:self-auto">
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
