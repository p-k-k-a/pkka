import Link from "next/link";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { AnnouncementsSection } from "@/components/announcements-section";

export default function HomePage() {
  return (
    <div>
      <section className="bg-muted/40 px-6 py-16 md:py-24">
        <div className="mx-auto max-w-7xl grid grid-cols-1 gap-12 items-center md:grid-cols-2">
          <div className="space-y-6 text-left">
            <p className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Wydział Informatyki AGH
            </p>

            <h1 className="text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl md:text-6xl">
              Witaj w PKKA
            </h1>

            <p className="max-w-lg text-xl text-muted-foreground">
              Oficjalny portal Klubu Alumna Wydziału Informatyki AGH. Buduj sieć kontaktów,
              wracaj do wspomnień i bądź na bieżąco z życiem wydziału.
            </p>

            <div className="flex flex-col gap-4 pt-4 sm:flex-row">
              <Button asChild size="xl" className="rounded-lg text-base px-6">
                <Link href="/register">Dołącz do nas</Link>
              </Button>

              <Button asChild variant="outline" size="xl" className="rounded-lg text-base px-6">
                <Link href="/info">Dowiedz się więcej</Link>
              </Button>
            </div>
          </div>

          <div className="relative aspect-[4/3] rounded-2xl bg-muted shadow-xl border-4 border-background overflow-hidden flex items-center justify-center text-muted-foreground">
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
