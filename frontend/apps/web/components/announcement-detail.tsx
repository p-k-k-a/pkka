"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useGetBlogPostBySlug } from "@pkka/api";
import { useEffect } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { Avatar } from "@/components/ui/avatar";

function announcementImageSrc(imageUrl: string) {
  return imageUrl.startsWith("/") || imageUrl.startsWith("http") ? imageUrl : "/hero.png";
}

function formatPublishedAt(publishedAt: string) {
  const date = new Date(publishedAt);
  if (Number.isNaN(date.getTime())) {
    return { dateLabel: "Nieznana data", timeLabel: "--:--" };
  }

  const dateLabel = new Intl.DateTimeFormat("pl-PL", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(date);

  const timeLabel = new Intl.DateTimeFormat("pl-PL", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);

  return { dateLabel, timeLabel };
}

function AnnouncementContent({ content }: { content: string }) {
  const paragraphs = content.split(/\n\n+/).filter((p) => p.trim().length > 0);

  if (paragraphs.length === 0) {
    return <p className="text-base leading-8 text-foreground/90">{content}</p>;
  }

  return (
    <div className="space-y-6">
      {paragraphs.map((paragraph, index) => (
        <p key={index} className="text-base leading-8 text-foreground/90">
          {paragraph.trim()}
        </p>
      ))}
    </div>
  );
}

function HeaderBar() {
  return (
    <div className="mb-8 flex items-center justify-between border-b border-border/70 pb-4">
      <Button asChild variant="ghost" size="icon" className="rounded-full text-muted-foreground hover:text-foreground">
        <Link href="/">
          <ArrowLeft className="size-5" />
          <span className="sr-only">Wróć</span>
        </Link>
      </Button>
      <h2 className="text-sm font-bold tracking-widest text-foreground uppercase">
        SZCZEGÓŁY WPISU
      </h2>
      <div className="w-10" aria-hidden="true" />
    </div>
  );
}

function BackLink({ className }: { className?: string }) {
  return (
    <Button asChild variant="outline" className={cn("rounded-xl font-semibold", className)}>
      <Link href="/">
        <ArrowLeft className="size-4 mr-2" />
        Wróć do aktualności
      </Link>
    </Button>
  );
}


export function AnnouncementDetail({ slug }: { slug: string }) {
  const { data: response, isLoading, isError, isFetching } = useGetBlogPostBySlug(slug);
  const post = response?.data;
  const publication = post ? formatPublishedAt(post.publishedAt) : null;

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [slug]);

  if (isLoading && !post) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10 md:py-14">
        <div className="h-10 border-b mb-8 flex items-center justify-between">
          <Skeleton className="size-8 rounded-full" />
          <Skeleton className="h-4 w-32" />
          <div className="w-8" />
        </div>
        <Skeleton className="aspect-[16/9] w-full rounded-2xl mb-8" />
        <Skeleton className="h-6 w-32 mb-4" />
        <Skeleton className="h-12 w-full max-w-2xl mb-8" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="text-destructive font-semibold mb-6">Nie udało się załadować ogłoszenia.</p>
        <BackLink />
      </div>
    );
  }

  if (!post) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <h1 className="text-2xl font-bold mb-2">Nie znaleziono ogłoszenia</h1>
        <p className="text-muted-foreground mb-8">
          {isFetching
            ? "Szukamy ogłoszenia…"
            : "To ogłoszenie nie istnieje lub link jest nieaktualny."}
        </p>
        <BackLink />
      </div>
    );
  }

  return (
    <article className="mx-auto max-w-3xl px-6 py-10 md:py-14">
      <HeaderBar />

      <div className="relative mb-8 aspect-[16/9] overflow-hidden rounded-2xl border bg-muted shadow-sm">
        <Image
          src={announcementImageSrc(post.imageUrl)}
          alt={post.title}
          fill
          priority
          sizes="(max-width: 768px) 100vw, 768px"
          className="object-cover"
        />
      </div>

      <header className="space-y-4 mb-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
            <span>{publication?.dateLabel}</span>
            <span>-</span>
            <span>{publication?.timeLabel}</span>
          </div>
          <Badge variant="secondary" className="shadow-none rounded-md px-2.5 py-0.5 text-xs font-semibold">
            {post.tag}
          </Badge>
        </div>

        <h1 className="text-2xl font-extrabold tracking-tight text-foreground md:text-3xl md:leading-tight">
          {post.title}
        </h1>

        <div className="pt-4 flex items-center gap-3">
          <Avatar
            src={post.author?.avatarUrl}
            fallback={post.author?.name}
            alt={post.author?.name}
            size="lg"
          />
          <div>
            <div className="text-base font-bold text-foreground">
              {post.author?.name}
            </div>
            <div className="text-xs font-bold tracking-wider text-muted-foreground uppercase">
              {post.author?.role}
            </div>
          </div>
        </div>
      </header>

      <div className="mt-8 space-y-6 border-t border-border/70 pt-8">
        <div className="space-y-2">
          <h3 className="text-xs font-extrabold tracking-widest text-muted-foreground uppercase">
            O WPISIE
          </h3>
          <p className="text-lg font-medium leading-relaxed text-foreground">
            {post.description}
          </p>
        </div>

        <div className="pt-4">
          <AnnouncementContent content={post.content} />
        </div>
      </div>

      <footer className="mt-16 flex justify-center border-t pt-10">
        <BackLink />
      </footer>
    </article>
  );
}

