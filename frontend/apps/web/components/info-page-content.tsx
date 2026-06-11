import Link from "next/link";
import { GraduationCap, Handshake, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

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
    <div className="mx-auto max-w-4xl px-6 py-12 md:py-20">
      <header className="mb-12 space-y-6 md:mb-16">
        <p className="text-muted-foreground text-sm font-semibold tracking-wider uppercase">
          Wydział Informatyki AGH
        </p>
        <h1 className="text-foreground text-4xl font-extrabold tracking-tight md:text-5xl">
          Klub Alumnów
        </h1>
        <div className="text-muted-foreground space-y-4 text-lg leading-relaxed">
          <p>Tu zacznie się Twoja kolejna ścieżka rozwoju. Z ludźmi, którym zależy.</p>
          <p>
            Zostań częścią społeczności, która będzie wspierać, inspirować i otwierać drzwi do
            nowych możliwości.
          </p>
          <p>
            Do Klubu mogą dołączyć absolwentki i absolwenci kierunków informatycznych AGH – także
            studiów podyplomowych. Szczegółowe informacje znajdziesz poniżej.
          </p>
        </div>
      </header>

      <section className="mb-12 space-y-6 md:mb-16">
        <h2 className="text-foreground text-2xl font-extrabold tracking-tight md:text-3xl">
          Czym jest Klub Alumnów WI AGH?
        </h2>
        <div className="text-muted-foreground space-y-4 text-base leading-8 md:text-lg">
          <p>
            To nie będzie kolejna formalna organizacja. Ideą Klubu Alumnów WI AGH jest zbudowanie
            społeczności ludzi, którzy chcą się rozwijać, wspierać i realnie pomagać sobie w
            karierze.
          </p>
          <p>
            Spotkasz tu osoby, które przeszły podobną drogę – od juniora do managera, od inżyniera
            do lidera. Znajdziesz tu mentoring, wartościowe kontakty i realne wsparcie w środowisku,
            w którym wymiana wiedzy po prostu działa.
          </p>
        </div>
      </section>

      <section className="mb-12 grid gap-6 sm:grid-cols-3 md:mb-16">
        {HIGHLIGHTS.map(({ icon: Icon, title, description }) => (
          <Card
            key={title}
            className="border-border/70 bg-card gap-3 rounded-2xl border p-6 shadow-sm"
          >
            <div className="bg-muted flex size-10 items-center justify-center rounded-lg">
              <Icon className="text-foreground size-5" aria-hidden="true" />
            </div>
            <h3 className="text-foreground text-lg font-bold">{title}</h3>
            <p className="text-muted-foreground text-sm leading-relaxed">{description}</p>
          </Card>
        ))}
      </section>

      <section className="border-border/70 bg-muted/40 rounded-2xl border px-8 py-10 text-center">
        <h2 className="text-foreground mb-3 text-xl font-bold md:text-2xl">Gotowy, by dołączyć?</h2>
        <p className="text-muted-foreground mx-auto mb-6 max-w-lg text-sm leading-relaxed md:text-base">
          Wypełnij formularz rejestracyjny i zostań częścią społeczności absolwentów Wydziału
          Informatyki AGH.
        </p>
        <Button asChild size="xl" variant="outline" className="rounded-xl font-semibold">
          <Link href="/register">Przejdź do rejestracji</Link>
        </Button>
      </section>
    </div>
  );
}
