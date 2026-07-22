import Link from "next/link";
import { Pencil, UserRound } from "lucide-react";
import type { MeResponse, ProfileResponse } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { ContactActions } from "@/components/profile/contact-actions";
import { ProfileSectionCard } from "@/components/profile/profile-section-card";
import { SkillChips } from "@/components/profile/skill-chips";
import type { ProfileMockFields } from "@/lib/profile-mock";

type ProfileViewProps = {
  me: MeResponse;
  profile: ProfileResponse;
  mock: ProfileMockFields;
};

export function ProfileView({ me, profile, mock }: ProfileViewProps) {
  const fullName = [me.firstName, me.lastName].filter(Boolean).join(" ").trim();
  const showName = mock.visibility.name && fullName.length > 0;
  const headline =
    profile.currentPosition || profile.company
      ? [profile.currentPosition, profile.company].filter(Boolean).join(" @ ")
      : null;
  const education =
    mock.fieldOfStudy && mock.graduationYear
      ? `Absolwent: ${mock.fieldOfStudy} — ${mock.graduationYear}`
      : null;

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8">
      <div className="flex flex-col gap-4">
        <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
          Profil
        </h1>
        <Button asChild variant="outline" size="sm" className="w-fit uppercase tracking-wider">
          <Link href="/dashboard/profile/edit">
            <Pencil data-icon="inline-start" />
            Edytuj profil
          </Link>
        </Button>
      </div>

      <div className="border-border bg-muted flex aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-xl border">
        <UserRound className="text-muted-foreground size-24" strokeWidth={1.5} aria-hidden="true" />
      </div>

      <div className="flex flex-col gap-1">
        {showName ? (
          <h2 className="font-heading text-foreground text-[23px] leading-tight font-semibold md:text-[28px]">
            {fullName}
          </h2>
        ) : null}
        {mock.alumnSince ? (
          <p className="text-muted-foreground text-[10px] font-semibold tracking-widest uppercase">
            Alumn od {mock.alumnSince}
          </p>
        ) : null}
        {headline ? (
          <p className="text-muted-foreground text-lg">
            {profile.currentPosition}
            {profile.currentPosition && profile.company ? " @ " : ""}
            {profile.company ? (
              <span className="text-foreground font-semibold">{profile.company}</span>
            ) : null}
          </p>
        ) : null}
        {education ? <p className="text-muted-foreground text-sm">{education}</p> : null}
      </div>

      <ContactActions
        email={me.email}
        showEmail={mock.visibility.email}
        discordId={mock.discordId}
        showDiscord={mock.visibility.discord}
        linkedinUrl={profile.linkedinUrl}
        githubUrl={profile.githubUrl}
      />

      {mock.bio ? (
        <ProfileSectionCard title="O mnie">
          <p className="text-muted-foreground leading-7">{mock.bio}</p>
        </ProfileSectionCard>
      ) : null}

      {profile.tags.length > 0 ? (
        <ProfileSectionCard title="Umiejętności">
          <SkillChips tags={profile.tags} />
        </ProfileSectionCard>
      ) : null}
    </div>
  );
}
