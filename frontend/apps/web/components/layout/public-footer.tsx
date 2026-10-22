import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { PRIVACY_PATH, TERMS_PATH } from "@pkka/domain";

export function PublicFooter() {
  return (
    <footer className="bg-band text-band-foreground px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto flex max-w-[1280px] flex-col gap-8 md:flex-row md:items-end md:justify-between">
        <div className="space-y-3">
          <p className="font-heading text-[23px] font-semibold tracking-tight md:text-[28px]">
            PKKA
          </p>
          <p className="text-band-foreground/80 max-w-md text-sm leading-relaxed md:text-base">
            Klub Alumnów Wydziału Informatyki AGH — społeczność absolwentów, mentoring i networking.
          </p>
        </div>
        <div className="space-y-2 text-sm md:text-right">
          <Link
            href="/info"
            className="font-heading text-band-foreground inline-flex items-center gap-2 hover:underline"
          >
            Dowiedz się więcej
            <ArrowRight className="size-4" aria-hidden="true" />
          </Link>
          <p className="text-band-foreground/80 flex gap-4 md:justify-end">
            <Link href={TERMS_PATH} className="hover:underline">
              Regulamin
            </Link>
            <Link href={PRIVACY_PATH} className="hover:underline">
              Ochrona danych osobowych
            </Link>
          </p>
          <p className="text-band-foreground/70">© 2026 Klub Alumnów WI AGH</p>
        </div>
      </div>
    </footer>
  );
}
