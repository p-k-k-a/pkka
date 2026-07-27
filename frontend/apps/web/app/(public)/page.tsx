"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AnnouncementsSection } from "@/components/announcements-section";

export default function HomePage() {
  return (
    <div>
      <section className="bg-muted px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto grid max-w-[1280px] grid-cols-1 items-center gap-10 md:grid-cols-2 md:gap-16">
          <div className="space-y-6 text-left">
            <p className="text-muted-foreground text-sm font-semibold tracking-wider uppercase">
              Wydział Informatyki AGH
            </p>

            <h1 className="font-heading text-foreground text-[33px] leading-tight font-semibold tracking-tight md:text-[48px]">
              Witaj w Klubie Alumnów
            </h1>

            <p className="text-muted-foreground max-w-lg text-base leading-relaxed md:text-lg">
              Oficjalny portal Klubu Alumnów Wydziału Informatyki AGH. Buduj sieć kontaktów, wracaj
              do wspomnień i bądź na bieżąco z życiem wydziału.
            </p>

            <div className="flex flex-col gap-3 pt-2 sm:flex-row sm:items-center">
              <Button asChild size="xl" className="gap-2">
                <Link href="/login">
                  Dołącz do nas
                  <ArrowRight data-icon="inline-end" />
                </Link>
              </Button>

              <Button asChild variant="link" size="xl" className="text-accent px-0">
                <Link href="/info">
                  Dowiedz się więcej
                  <ArrowRight data-icon="inline-end" />
                </Link>
              </Button>
            </div>
          </div>

          <div className="relative aspect-[4/3] overflow-hidden rounded-lg">
            <Image
              src="/hero.png"
              alt="Wydział Informatyki AGH"
              fill
              sizes="(max-width: 768px) 100vw, (max-width: 1280px) 50vw, 640px"
              className="object-cover"
              priority
              fetchPriority="high"
            />
          </div>
        </div>
      </section>

      <section className="bg-background">
        <AnnouncementsSection />
      </section>
    </div>
  );
}
