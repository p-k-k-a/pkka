"use client";

import Link from "next/link";
import {
  ArrowRight,
  BookOpen,
  Briefcase,
  CalendarCheck,
  EyeOff,
  GraduationCap,
  Mail,
} from "lucide-react";
import type { AlumniProfileResponse, ProfileResponse } from "@pkka/api";
import { DetailBackLink } from "@/components/content/detail-back-link";
import { ProfileAside, ProfileAsideItem } from "@/components/profile/profile-aside";
import { ProfileContactList } from "@/components/profile/profile-contact-list";
import { SectionHeading } from "@/components/profile/section-heading";
import { SkillChips } from "@/components/profile/skill-chips";
import { VisibilitySummary } from "@/components/profile/visibility-summary";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { DiscordIcon } from "@pkka/icons/web";
import { getProfileContacts } from "@pkka/domain";

const EDIT_HREF = "/dashboard/profile/edit";

/**
 * The viewer's own profile (`/api/profiles/me`) and another alumn's public profile
 * (`/api/alumni/{id}`) carry the same fields, so one view renders both. The public
 * one already has whatever the owner hid nulled out by the backend.
 */
type ProfileViewProps =
  | { variant: "own"; profile: ProfileResponse }
  | { variant: "alumn"; profile: AlumniProfileResponse; directoryHref: string };

function EmptyHint({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-muted-foreground text-sm">
      {children}{" "}
      <Link href={EDIT_HREF} className="text-accent font-semibold underline">
        Uzupełnij profil
      </Link>
    </p>
  );
}

export function ProfileView(props: ProfileViewProps) {
  const { profile } = props;
  const isOwn = props.variant === "own";
  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(" ").trim();
  const contacts = getProfileContacts(profile);
  const alumnSinceYear = profile.alumnSince ? profile.alumnSince.slice(0, 4) : null;
  const role = [profile.currentPosition, profile.company].filter(Boolean).join(" · ");
  const nameHidden = isOwn && !profile.visibility.name && fullName.length > 0;

  // Someone else's empty section is just noise, while the owner gets a nudge to fill it.
  const showBio = isOwn || Boolean(profile.bio);
  const showSkills = isOwn || profile.tags.length > 0;
  const hasStudies = Boolean(profile.fieldOfStudy || profile.graduationYear || alumnSinceYear);

  // Same single primary CTA as an event's "Zapisz się": edit for the owner,
  // the most direct contact channel for everyone else.
  const primaryAction = isOwn ? (
    <Button asChild size="xl">
      <Link href={EDIT_HREF}>
        Edytuj profil
        <ArrowRight data-icon="inline-end" />
      </Link>
    </Button>
  ) : contacts.discordUrl ? (
    <Button asChild size="xl">
      <a href={contacts.discordUrl} target="_blank" rel="noopener noreferrer">
        <DiscordIcon className="size-5" />
        Napisz na Discordzie
      </a>
    </Button>
  ) : contacts.email ? (
    <Button asChild size="xl">
      <a href={`mailto:${contacts.email}`}>
        <Mail data-icon="inline-start" />
        Napisz e-mail
      </a>
    </Button>
  ) : null;

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-20">
      <div className="mx-auto max-w-[1280px]">
        {props.variant === "alumn" ? (
          <DetailBackLink href={props.directoryHref} label="Wróć do katalogu" />
        ) : null}

        <div
          className={`grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_320px] ${isOwn ? "" : "mt-10"}`}
        >
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="bg-band text-band-foreground inline-flex rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
                {profile.graduationYear ? `Rocznik ${profile.graduationYear}` : "Alumn"}
              </span>
              {profile.willingToMentor ? (
                <span className="bg-primary text-primary-foreground inline-flex items-center gap-1.5 rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
                  <GraduationCap className="size-3.5" aria-hidden="true" />
                  Mentor
                </span>
              ) : null}
            </div>

            <div className="mt-6 flex flex-col gap-6 sm:flex-row sm:items-center">
              <Avatar
                fallback={fullName || "Alumn"}
                alt={fullName}
                className="size-24 text-2xl font-semibold"
              />
              <div className="min-w-0">
                <h1 className="font-heading text-foreground text-[33px] leading-tight font-semibold tracking-tight md:text-[40px]">
                  {fullName || (isOwn ? "Twój profil" : "Alumn")}
                </h1>
                {nameHidden ? (
                  <p className="text-muted-foreground mt-1 inline-flex items-center gap-1.5 text-xs font-semibold tracking-widest uppercase">
                    <EyeOff className="size-3.5" aria-hidden="true" />
                    Imię i nazwisko ukryte przed innymi
                  </p>
                ) : null}
                {role ? (
                  <p className="text-muted-foreground mt-2 flex items-center gap-2 text-lg">
                    <Briefcase className="size-4 shrink-0" aria-hidden="true" />
                    {role}
                  </p>
                ) : null}
              </div>
            </div>

            {primaryAction ? <div className="mt-8">{primaryAction}</div> : null}

            {showBio ? (
              <section className="mt-16 space-y-6">
                <SectionHeading>O mnie</SectionHeading>
                {profile.bio ? (
                  <p className="text-muted-foreground max-w-2xl leading-relaxed whitespace-pre-line">
                    {profile.bio}
                  </p>
                ) : (
                  <EmptyHint>Nie masz jeszcze opisu.</EmptyHint>
                )}
              </section>
            ) : null}

            {showSkills ? (
              <section className="mt-12 space-y-6">
                <SectionHeading>Umiejętności</SectionHeading>
                {profile.tags.length > 0 ? (
                  <SkillChips tags={profile.tags} />
                ) : (
                  <EmptyHint>Nie wybrałeś jeszcze żadnych umiejętności.</EmptyHint>
                )}
              </section>
            ) : null}
          </div>

          <div className="flex flex-col gap-12 lg:sticky lg:top-24">
            {hasStudies ? (
              <ProfileAside title="Studia">
                {profile.fieldOfStudy ? (
                  <ProfileAsideItem icon={BookOpen} label="Kierunek">
                    {profile.fieldOfStudy}
                  </ProfileAsideItem>
                ) : null}
                {profile.graduationYear ? (
                  <ProfileAsideItem icon={GraduationCap} label="Rok ukończenia">
                    {profile.graduationYear}
                  </ProfileAsideItem>
                ) : null}
                {alumnSinceYear ? (
                  <ProfileAsideItem icon={CalendarCheck} label="Alumn od">
                    {alumnSinceYear}
                  </ProfileAsideItem>
                ) : null}
              </ProfileAside>
            ) : null}

            {contacts.hasAny ? (
              <ProfileAside title="Kontakt">
                <ProfileContactList contacts={contacts} />
              </ProfileAside>
            ) : isOwn ? (
              <ProfileAside title="Kontakt">
                <EmptyHint>Nie udostępniasz żadnej formy kontaktu.</EmptyHint>
              </ProfileAside>
            ) : null}

            {isOwn ? (
              <ProfileAside title="Widoczność">
                <p className="text-muted-foreground -mt-2 text-[13px]">
                  Tak Twoje dane widzą pozostali alumni.
                </p>
                <VisibilitySummary
                  visibility={profile.visibility}
                  discordConnected={Boolean(profile.discordId)}
                />
              </ProfileAside>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}
