"use client";

import Link from "next/link";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { AnnouncementsSection } from "@/components/announcements-section";
import { useAuth } from "@/lib/auth-context";

export default function HomePage() {
  const { loginWithKeycloak } = useAuth();

  return (
    <div>
      <section className="bg-muted/40 px-6 py-16 md:py-24">
        <div className="mx-auto grid max-w-7xl grid-cols-1 items-center gap-12 md:grid-cols-2">
          <div className="space-y-6 text-left">
            <p className="text-muted-foreground text-sm font-semibold tracking-wider uppercase">
              Wydział Informatyki AGH
            </p>

            <h1 className="text-foreground text-4xl font-extrabold tracking-tight sm:text-5xl md:text-6xl">
              Witaj w PKKA
            </h1>

            <p className="text-muted-foreground max-w-lg text-xl">
              Oficjalny portal Klubu Alumna Wydziału Informatyki AGH. Buduj sieć kontaktów, wracaj
              do wspomnień i bądź na bieżąco z życiem wydziału.
            </p>

            <div className="flex flex-col gap-4 pt-4 sm:flex-row">
              <Button
                onClick={loginWithKeycloak}
                size="xl"
                className="cursor-pointer rounded-lg px-6 text-base"
              >
                Dołącz do nas
              </Button>

              <Button asChild variant="outline" size="xl" className="rounded-lg px-6 text-base">
                <Link href="/info">Dowiedz się więcej</Link>
              </Button>
            </div>
          </div>

          <div className="bg-muted border-background text-muted-foreground relative flex aspect-[4/3] items-center justify-center overflow-hidden rounded-2xl border-4 shadow-xl">
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

      <AnnouncementsSection />
    </div>
  );
}
