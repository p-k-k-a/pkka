import { Briefcase, EyeOff, GraduationCap } from "lucide-react";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";

type ProfileHeroProps = {
  fullName: string;
  avatarUrl?: string | null;
  /** Rendered as an "ukryte" marker — the owner always sees their own name. */
  nameHidden?: boolean;
  currentPosition?: string;
  company?: string;
  alumnSinceYear?: string | null;
  willingToMentor?: boolean;
  action?: React.ReactNode;
};

export function ProfileHero({
  fullName,
  avatarUrl,
  nameHidden = false,
  currentPosition,
  company,
  alumnSinceYear,
  willingToMentor = false,
  action,
}: ProfileHeroProps) {
  const role = [currentPosition, company].filter(Boolean).join(" · ");

  return (
    <section className="bg-navy text-white-text">
      <div className="mx-auto flex max-w-[1280px] flex-col gap-8 px-4 py-10 md:flex-row md:items-center md:justify-between md:px-10 md:py-16">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
          {/* Both schemes are pinned to band tokens — the avatar's own hashed
              palette would fight the navy band. */}
          <Avatar
            src={avatarUrl ?? undefined}
            fallback={fullName}
            alt={fullName}
            className="border-white-text/20 bg-white-text/10 text-white-text dark:bg-white-text/10 dark:text-white-text size-24 text-2xl font-semibold md:size-28"
          />

          <div className="flex flex-col gap-2">
            {alumnSinceYear || willingToMentor ? (
              <div className="flex flex-wrap items-center gap-2">
                {alumnSinceYear ? (
                  <Badge className="bg-primary text-primary-foreground rounded-md px-2 py-0.5 text-[11px] font-bold tracking-widest uppercase">
                    Alumn od {alumnSinceYear}
                  </Badge>
                ) : null}
                {willingToMentor ? (
                  <Badge className="bg-white-text/10 text-white-text rounded-md px-2 py-0.5 text-[11px] font-bold tracking-widest uppercase">
                    <GraduationCap aria-hidden="true" />
                    Mentor
                  </Badge>
                ) : null}
              </div>
            ) : null}

            <div className="flex flex-wrap items-center gap-3">
              <h1 className="font-heading text-[28px] leading-tight font-semibold md:text-[33px]">
                {fullName || "Twój profil"}
              </h1>
              {nameHidden ? (
                <span className="text-white-text/60 inline-flex items-center gap-1 text-[11px] font-semibold tracking-widest uppercase">
                  <EyeOff className="size-3" aria-hidden="true" />
                  Ukryte
                </span>
              ) : null}
            </div>

            {role ? (
              <p className="text-white-text/80 flex items-center gap-2 text-[18px]">
                <Briefcase className="size-4 shrink-0" aria-hidden="true" />
                {role}
              </p>
            ) : null}
          </div>
        </div>

        {action ? <div className="shrink-0">{action}</div> : null}
      </div>
    </section>
  );
}
