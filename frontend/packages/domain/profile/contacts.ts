import type { ProfileResponse } from "@pkka/api";

const DISCORD_ID_PATTERN = /^[0-9]{5,32}$/;

// Profile URLs are user-supplied server data. Never hand a non-https URL to
// Linking.openURL, or a malicious profile could launch arbitrary schemes
// (tel:, third-party app deep links) on the viewer's device.
function isHttpsUrl(url: string) {
  return /^https:\/\//i.test(url);
}

function isDiscordId(id: string) {
  return DISCORD_ID_PATTERN.test(id);
}

type ProfileContactSource = Pick<
  ProfileResponse,
  "email" | "discordId" | "linkedinUrl" | "githubUrl" | "visibility"
>;

export type ProfileContacts = {
  email: string | null;
  discordId: string | null;
  discordUrl: string | null;
  linkedinUrl: string | null;
  githubUrl: string | null;
  hasAny: boolean;
};

export function getProfileContacts(profile: ProfileContactSource): ProfileContacts {
  const email = profile.visibility.email && profile.email ? profile.email : null;
  const discordId =
    profile.visibility.discord && profile.discordId && isDiscordId(profile.discordId)
      ? profile.discordId
      : null;
  const linkedinUrl =
    profile.linkedinUrl && isHttpsUrl(profile.linkedinUrl) ? profile.linkedinUrl : null;
  const githubUrl = profile.githubUrl && isHttpsUrl(profile.githubUrl) ? profile.githubUrl : null;

  return {
    email,
    discordId,
    discordUrl: discordId ? `https://discord.com/users/${discordId}` : null,
    linkedinUrl,
    githubUrl,
    hasAny: Boolean(email || discordId || linkedinUrl || githubUrl),
  };
}
