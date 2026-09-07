import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { eventsListHref, type EventPathVariant } from "@/lib/event-paths";

export function EventsArchiveBand({ variant = "public" }: { variant?: EventPathVariant }) {
  return (
    <section className="bg-navy text-white-text px-4 py-16 md:px-10 md:py-20">
      <div className="mx-auto max-w-[1280px] space-y-6">
        <p className="bg-accent/20 text-white-text inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
          Archiwum
        </p>
        <h2 className="font-heading text-[28px] leading-tight font-semibold tracking-tight md:text-[40px]">
          Szukasz wydarzenia archiwalnego?
        </h2>
        <p className="text-white-text/80 max-w-2xl text-base leading-relaxed">
          Zobacz nasze archiwum i wróć do poprzednich wydarzeń. Sprawdź informacje z klubowych
          spotkań.
        </p>
        <Button asChild variant="secondary" size="xl" className="gap-2">
          <Link href={eventsListHref(variant, true)}>
            Przejdź do archiwum
            <ArrowRight data-icon="inline-end" />
          </Link>
        </Button>
      </div>
    </section>
  );
}
