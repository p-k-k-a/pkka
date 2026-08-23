"use client";

import Link from "next/link";
import { ArrowRight, GraduationCap, Handshake, Users } from "lucide-react";
import { Button } from "@/components/ui/button";

const HIGHLIGHTS = [
  {
    icon: Users,
    title: "Społeczność",
    description:
      "Ludzie, którzy przeszli podobną drogę — od juniora do managera, od inżyniera do lidera.",
  },
  {
    icon: GraduationCap,
    title: "Mentoring",
    description:
      "Wymiana wiedzy i doświadczeń w środowisku, w którym wsparcie w karierze po prostu działa.",
  },
  {
    icon: Handshake,
    title: "Networking",
    description:
      "Wartościowe kontakty i realne wsparcie od absolwentek i absolwentów kierunków informatycznych AGH.",
  },
];

export function InfoPageContent() {
  return (
    <div>
      <section className="bg-muted px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <p className="text-muted-foreground text-sm font-semibold tracking-wider uppercase">
            Wydział Informatyki AGH
          </p>
          <h1 className="font-heading text-foreground text-h2-lg md:text-display max-w-3xl leading-tight font-semibold tracking-tight">
            Czym jest Klub Alumnów WI AGH?
          </h1>
          <div className="text-muted-foreground max-w-3xl space-y-4 text-base leading-relaxed md:text-lg">
            <p>Tu zacznie się Twoja kolejna ścieżka rozwoju. Z ludźmi, którym zależy.</p>
            <p>
              To nie będzie kolejna formalna organizacja. Ideą Klubu jest zbudowanie społeczności
              ludzi, którzy chcą się rozwijać, wspierać i realnie pomagać sobie w karierze.
            </p>
            <p>
              Do Klubu mogą dołączyć absolwentki i absolwenci kierunków informatycznych AGH – także
              studiów podyplomowych.
            </p>
          </div>
        </div>
      </section>

      <section className="bg-primary text-primary-foreground px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto grid max-w-[1280px] gap-8 md:grid-cols-3 md:gap-10">
          {HIGHLIGHTS.map(({ icon: Icon, title, description }) => (
            <div key={title} className="space-y-3">
              <div className="bg-primary-foreground/10 flex size-10 items-center justify-center rounded-lg">
                <Icon className="size-5" aria-hidden="true" />
              </div>
              <h2 className="font-heading text-h3 font-semibold">{title}</h2>
              <p className="text-sm leading-relaxed opacity-90 md:text-base">{description}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-background px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px] space-y-6 text-center">
          <h2 className="font-heading text-foreground text-h2 md:text-h2-lg font-semibold tracking-tight">
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
