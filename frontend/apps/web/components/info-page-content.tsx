"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { AppGuide } from "@/components/app-guide";
import { Button } from "@/components/ui/button";

export function InfoPageContent() {
  return (
    <div>
      <AppGuide />

      <section className="bg-muted px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px] space-y-6 text-center">
          <h2 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            Gotowy, by dołączyć?
          </h2>
          <p className="text-muted-foreground mx-auto max-w-lg text-base leading-relaxed">
            Wypełnij formularz rejestracyjny i zostań częścią społeczności absolwentów Wydziału
            Informatyki AGH.
          </p>
          <Button asChild size="xl" className="gap-2">
            <Link href="/login">
              Przejdź do rejestracji
              <ArrowRight data-icon="inline-end" />
            </Link>
          </Button>
        </div>
      </section>
    </div>
  );
}
