import Link from "next/link";
import { ArrowRight, Briefcase, GraduationCap, Sparkles } from "lucide-react";
import type { AlumniListItemResponse } from "@pkka/api";
import { Avatar } from "@/components/ui/avatar";
import { Card } from "@/components/ui/card";

const VISIBLE_TAGS = 3;

// Mirrors EventCalendarCard: flat muted tile, band chip, icon rows, "read more" footer.
export function AlumniCard({ alumn }: { alumn: AlumniListItemResponse }) {
  const name = [alumn.firstName, alumn.lastName].filter(Boolean).join(" ") || "Alumn";
  const role = [alumn.currentPosition, alumn.company].filter(Boolean).join(" · ");
  const tags = alumn.tags.map((tag) => tag.name);
  const tagsLabel =
    tags.slice(0, VISIBLE_TAGS).join(", ") +
    (tags.length > VISIBLE_TAGS ? ` +${tags.length - VISIBLE_TAGS}` : "");

  return (
    <Link
      href={`/dashboard/alumni/${alumn.id}`}
      className="group focus-visible:ring-ring block h-full rounded-none transition-opacity hover:opacity-90 focus-visible:ring-2 focus-visible:outline-none"
    >
      <Card className="bg-muted flex h-full flex-col gap-6 overflow-visible rounded-none p-6 shadow-none ring-0">
        <div className="flex flex-wrap items-center gap-2">
          <span className="bg-band text-band-foreground inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
            {alumn.graduationYear ? `Rocznik ${alumn.graduationYear}` : "Alumn"}
          </span>
          {alumn.willingToMentor ? (
            <span className="bg-primary text-primary-foreground inline-flex w-fit items-center gap-1 rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
              <GraduationCap className="size-3.5" aria-hidden="true" />
              Mentor
            </span>
          ) : null}
        </div>

        <div className="flex items-center gap-4">
          <Avatar fallback={name} alt="" size="lg" />
          <h3 className="font-heading text-foreground text-lg leading-snug font-semibold">
            {name}
          </h3>
        </div>

        {role || tagsLabel ? (
          <ul className="text-muted-foreground space-y-3 text-sm">
            {role ? (
              <li className="flex items-start gap-3">
                <Briefcase className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                <span>{role}</span>
              </li>
            ) : null}
            {tagsLabel ? (
              <li className="flex items-start gap-3">
                <Sparkles className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                <span>{tagsLabel}</span>
              </li>
            ) : null}
          </ul>
        ) : null}

        <div className="mt-auto flex justify-end pt-2">
          <span className="bg-background text-accent inline-flex h-[34px] items-center gap-1.5 rounded-none px-2.5 text-sm font-medium">
            Zobacz profil
            <ArrowRight className="size-3.5" aria-hidden="true" />
          </span>
        </div>
      </Card>
    </Link>
  );
}
