"use client";

import Link from "next/link";
import { ArrowRight, Eye, Mail, Sparkles, UserRound } from "lucide-react";
import type { ProfileResponse } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { ContactActions } from "@/components/profile/contact-actions";
import { ProfileHero } from "@/components/profile/profile-hero";
import { ProfileSectionCard } from "@/components/profile/profile-section-card";
import { SkillChips } from "@/components/profile/skill-chips";
import { VisibilitySummary } from "@/components/profile/visibility-summary";
import { useProfileAvatar } from "@/lib/profile-avatar";
import { getProfileContacts } from "@/lib/profile-contacts";

const EDIT_HREF = "/dashboard/profile/edit";

function FactsBand({ facts }: { facts: { label: string; value: string }[] }) {
  return (
    <section className="bg-muted border-border border-b">
      <div className="mx-auto grid max-w-[1280px] gap-8 px-4 py-8 md:grid-cols-3 md:px-10">
        {facts.map((fact) => (
          <div key={fact.label} className="flex flex-col gap-1">
            <span className="font-heading text-foreground text-[23px] leading-tight font-semibold">
              {fact.value}
            </span>
            <span className="text-muted-foreground text-[11px] font-bold tracking-widest uppercase">
              {fact.label}
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}

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

export function ProfileView({ profile }: { profile: ProfileResponse }) {
  const avatarUrl = useProfileAvatar();
  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(" ").trim();
  const contacts = getProfileContacts(profile);
  const alumnSinceYear = profile.alumnSince ? profile.alumnSince.slice(0, 4) : null;

  const facts = [
    { label: "Kierunek", value: profile.fieldOfStudy },
    { label: "Rok ukończenia", value: profile.graduationYear?.toString() },
    { label: "Alumn od", value: alumnSinceYear },
  ].filter((fact): fact is { label: string; value: string } => Boolean(fact.value));

  return (
    <div className="flex flex-col">
      <ProfileHero
        fullName={fullName}
        avatarUrl={avatarUrl}
        nameHidden={!profile.visibility.name && fullName.length > 0}
        currentPosition={profile.currentPosition}
        company={profile.company}
        alumnSinceYear={alumnSinceYear}
        willingToMentor={profile.willingToMentor}
        action={
          <Button asChild size="xl" className="w-full font-bold md:w-auto">
            <Link href={EDIT_HREF}>
              Edytuj profil
              <ArrowRight data-icon="inline-end" />
            </Link>
          </Button>
        }
      />

      {facts.length > 0 ? <FactsBand facts={facts} /> : null}

      <section className="bg-background">
        <div className="mx-auto grid max-w-[1280px] items-start gap-6 px-4 py-10 md:px-10 md:py-12 lg:grid-cols-3">
          <div className="flex flex-col gap-6 lg:col-span-2">
            <ProfileSectionCard title="O mnie" icon={UserRound}>
              {profile.bio ? (
                <p className="text-muted-foreground text-[15px] leading-relaxed whitespace-pre-line">
                  {profile.bio}
                </p>
              ) : (
                <EmptyHint>Nie masz jeszcze opisu.</EmptyHint>
              )}
            </ProfileSectionCard>

            <ProfileSectionCard title="Umiejętności" icon={Sparkles}>
              {profile.tags.length > 0 ? (
                <SkillChips tags={profile.tags} />
              ) : (
                <EmptyHint>Nie wybrałeś jeszcze żadnych umiejętności.</EmptyHint>
              )}
            </ProfileSectionCard>
          </div>

          <div className="flex flex-col gap-6">
            <ProfileSectionCard title="Kontakt" icon={Mail}>
              {contacts.hasAny ? (
                <ContactActions contacts={contacts} />
              ) : (
                <EmptyHint>Nie udostępniasz żadnej formy kontaktu.</EmptyHint>
              )}
            </ProfileSectionCard>

            <ProfileSectionCard
              title="Widoczność"
              icon={Eye}
              description="Tak Twoje dane widzą pozostali alumni."
            >
              <VisibilitySummary
                visibility={profile.visibility}
                discordConnected={Boolean(profile.discordId)}
              />
            </ProfileSectionCard>
          </div>
        </div>
      </section>
    </div>
  );
}
