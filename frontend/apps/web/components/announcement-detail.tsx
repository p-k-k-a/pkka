"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useGetPost } from "@pkka/api";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { formatPublishedAt } from "@/lib/format-published-at";

const DEFAULT_IMAGE = "/hero.png";

function AnnouncementContent({ content }: { content: string }) {
  const paragraphs = content.split(/\n\n+/).filter((p) => p.trim().length > 0);

  if (paragraphs.length === 0) {
    return <p className="text-foreground/90 text-base leading-8">{content}</p>;
  }

  return (
    <div className="space-y-6">
      {paragraphs.map((paragraph, index) => (
        <p key={index} className="text-foreground/90 text-base leading-8">
          {paragraph.trim()}
        </p>
      ))}
    </div>
  );
}

function HeaderBar() {
  return (
    <div className="border-border/70 mb-8 flex items-center justify-between border-b pb-4">
      <Button
        asChild
        variant="ghost"
        size="icon"
        className="text-muted-foreground hover:text-foreground rounded-full"
      >
        <Link href="/">
          <ArrowLeft className="size-5" />
          <span className="sr-only">Wróć</span>
        </Link>
      </Button>
      <h2 className="text-foreground text-sm font-bold tracking-widest uppercase">
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
        <ArrowLeft className="mr-2 size-4" />
        Wróć do aktualności
      </Link>
    </Button>
  );
}

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
        <div className="mb-8 flex h-10 items-center justify-between border-b">
          <Skeleton className="size-8 rounded-full" />
          <Skeleton className="h-4 w-32" />
          <div className="w-8" />
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
        <BackLink />
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
        <BackLink />
      </div>
    );
  }

  return (
    <article className="mx-auto max-w-3xl px-6 py-10 md:py-14">
      <HeaderBar />

      <div className="bg-muted relative mb-8 aspect-[16/9] overflow-hidden rounded-2xl border shadow-sm">
        <Image
          src={DEFAULT_IMAGE}
          alt={post.title ?? "Aktualność"}
          fill
          priority
          sizes="(max-width: 768px) 100vw, 768px"
          className="object-cover"
        />
      </div>

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
        {post.content ? (
          <AnnouncementContent content={post.content} />
        ) : null}
      </div>

      <footer className="mt-16 flex justify-center border-t pt-10">
        <BackLink />
      </footer>
    </article>
  );
}
