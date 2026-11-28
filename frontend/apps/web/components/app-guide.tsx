import {
  APP_GUIDE_ADMIN_SECTION,
  APP_GUIDE_INTRO,
  APP_GUIDE_SECTIONS,
  APP_GUIDE_STEPS,
  type AppGuideIcon,
} from "@pkka/domain";
import { DiscordIcon } from "@pkka/icons/web";
import {
  BadgeCheck,
  Bell,
  CalendarDays,
  FolderOpen,
  Hash,
  Megaphone,
  Newspaper,
  ShieldCheck,
  UserRound,
  Users,
  UsersRound,
} from "lucide-react";

const ICONS: Record<AppGuideIcon, React.ComponentType<{ className?: string }>> = {
  news: Newspaper,
  calendar: CalendarDays,
  users: Users,
  user: UserRound,
  folder: FolderOpen,
  groups: UsersRound,
  bell: Bell,
  shield: ShieldCheck,
  megaphone: Megaphone,
  discord: DiscordIcon,
  badge: BadgeCheck,
  hash: Hash,
};

export function AppGuide({ showAdmin = false }: { showAdmin?: boolean }) {
  const sections = showAdmin
    ? [...APP_GUIDE_SECTIONS, APP_GUIDE_ADMIN_SECTION]
    : APP_GUIDE_SECTIONS;

  return (
    <>
      <section className="bg-muted px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <p className="text-muted-foreground text-sm font-semibold tracking-wider uppercase">
            {APP_GUIDE_INTRO.eyebrow}
          </p>
          <h1 className="font-heading text-foreground max-w-3xl text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
            {APP_GUIDE_INTRO.title}
          </h1>
          <div className="text-muted-foreground max-w-3xl space-y-4 text-base leading-relaxed md:text-lg">
            {APP_GUIDE_INTRO.paragraphs.map((paragraph) => (
              <p key={paragraph}>{paragraph}</p>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-background px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px] space-y-14">
          <div className="space-y-6">
            <h2 className="font-heading text-foreground text-[23px] font-semibold">Jak zacząć</h2>
            <ol className="grid gap-6 md:grid-cols-3">
              {APP_GUIDE_STEPS.map((step, index) => (
                <li key={step.title} className="bg-muted space-y-3 rounded-lg p-6">
                  <span className="bg-primary text-primary-foreground font-heading flex size-8 items-center justify-center rounded-full text-sm font-semibold">
                    {index + 1}
                  </span>
                  <p className="font-heading text-foreground text-lg font-semibold">{step.title}</p>
                  <p className="text-muted-foreground text-sm leading-relaxed">
                    {step.description}
                  </p>
                </li>
              ))}
            </ol>
          </div>

          {sections.map((section) => (
            <div key={section.title} className="space-y-6">
              <div className="space-y-2">
                <h2 className="font-heading text-foreground text-[23px] font-semibold">
                  {section.title}
                </h2>
                {section.description ? (
                  <p className="text-muted-foreground text-base">{section.description}</p>
                ) : null}
              </div>
              <ul className="grid gap-x-10 gap-y-6 sm:grid-cols-2 lg:grid-cols-3">
                {section.features.map((feature) => {
                  const Icon = ICONS[feature.icon];
                  return (
                    <li key={feature.title} className="flex gap-4">
                      <div className="bg-muted text-foreground flex size-10 shrink-0 items-center justify-center rounded-lg">
                        <Icon className="size-5" aria-hidden="true" />
                      </div>
                      <div className="space-y-1">
                        <p className="text-foreground font-semibold">{feature.title}</p>
                        <p className="text-muted-foreground text-sm leading-relaxed">
                          {feature.description}
                        </p>
                      </div>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
