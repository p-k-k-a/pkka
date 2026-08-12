import type { ProfileResponse } from "@pkka/api";
import { isHttpsUrl } from "@/lib/utils";

const DISCORD_ID_PATTERN = /^[0-9]{5,32}$/;

export type ProfileContacts = {
  email: string | null;
  discordUrl: string | null;
  linkedinUrl: string | null;
  githubUrl: string | null;
  hasAny: boolean;
};

export function getProfileContacts(profile: ProfileResponse): ProfileContacts {
  const email = profile.visibility.email && profile.email ? profile.email : null;
  const discordUrl =
    profile.visibility.discord && profile.discordId && DISCORD_ID_PATTERN.test(profile.discordId)
      ? `https://discord.com/users/${profile.discordId}`
      : null;
  const linkedinUrl =
    profile.linkedinUrl && isHttpsUrl(profile.linkedinUrl) ? profile.linkedinUrl : null;
  const githubUrl = profile.githubUrl && isHttpsUrl(profile.githubUrl) ? profile.githubUrl : null;

  return {
    email,
    discordUrl,
    linkedinUrl,
    githubUrl,
    hasAny: Boolean(email || discordUrl || linkedinUrl || githubUrl),
  };
}
