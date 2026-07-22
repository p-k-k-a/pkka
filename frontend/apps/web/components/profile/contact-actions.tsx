import { Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DiscordIcon } from "@/components/auth/discord-icon";
import { GithubIcon, LinkedinIcon } from "@/components/profile/social-icons";
import { isHttpsUrl } from "@/lib/profile-mock";

type ContactActionsProps = {
  email?: string;
  showEmail: boolean;
  discordId?: string;
  showDiscord: boolean;
  linkedinUrl?: string;
  githubUrl?: string;
};

const isDiscordId = (id: string) => /^[0-9]{5,32}$/.test(id);

export function ContactActions({
  email,
  showEmail,
  discordId,
  showDiscord,
  linkedinUrl,
  githubUrl,
}: ContactActionsProps) {
  const safeLinkedin = linkedinUrl && isHttpsUrl(linkedinUrl) ? linkedinUrl : null;
  const safeGithub = githubUrl && isHttpsUrl(githubUrl) ? githubUrl : null;
  const safeDiscord = showDiscord && discordId && isDiscordId(discordId) ? discordId : null;
  const canEmail = showEmail && !!email;

  if (!canEmail && !safeDiscord && !safeLinkedin && !safeGithub) return null;

  return (
    <div className="flex flex-col gap-5">
      {safeDiscord || canEmail ? (
        <div className="flex flex-col gap-3">
          {safeDiscord ? (
            <Button asChild size="xl" className="w-full font-bold">
              <a
                href={`https://discord.com/users/${safeDiscord}`}
                target="_blank"
                rel="noopener noreferrer"
              >
                <DiscordIcon className="size-5" />
                Kontakt przez Discord
              </a>
            </Button>
          ) : null}
          {canEmail ? (
            <Button asChild size="xl" variant="secondary" className="w-full font-bold">
              <a href={`mailto:${email}`}>
                <Mail data-icon="inline-start" />
                Kontakt przez E-mail
              </a>
            </Button>
          ) : null}
        </div>
      ) : null}

      {safeLinkedin || safeGithub ? (
        <div className="flex flex-wrap gap-6">
          {safeLinkedin ? (
            <a
              href={safeLinkedin}
              target="_blank"
              rel="noopener noreferrer"
              className="text-accent inline-flex items-center gap-1.5 text-sm font-semibold tracking-widest uppercase underline"
            >
              <LinkedinIcon className="size-4" />
              LinkedIn
            </a>
          ) : null}
          {safeGithub ? (
            <a
              href={safeGithub}
              target="_blank"
              rel="noopener noreferrer"
              className="text-accent inline-flex items-center gap-1.5 text-sm font-semibold tracking-widest uppercase underline"
            >
              <GithubIcon className="size-4" />
              GitHub
            </a>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
