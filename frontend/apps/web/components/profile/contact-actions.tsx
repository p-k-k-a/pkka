import { Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DiscordIcon } from "@/components/auth/discord-icon";
import { GithubIcon, LinkedinIcon } from "@/components/profile/social-icons";
import type { ProfileContacts } from "@/lib/profile-contacts";

export function ContactActions({ contacts }: { contacts: ProfileContacts }) {
  const { email, discordUrl, linkedinUrl, githubUrl } = contacts;

  if (!contacts.hasAny) return null;

  return (
    <div className="flex flex-col gap-4">
      {discordUrl ? (
        <Button asChild size="xl" className="w-full font-bold">
          <a href={discordUrl} target="_blank" rel="noopener noreferrer">
            <DiscordIcon className="size-5" />
            Kontakt przez Discord
          </a>
        </Button>
      ) : null}

      {email ? (
        <Button asChild size="xl" variant="secondary" className="w-full font-bold">
          <a href={`mailto:${email}`}>
            <Mail data-icon="inline-start" />
            {email}
          </a>
        </Button>
      ) : null}

      {linkedinUrl || githubUrl ? (
        <div className="flex flex-wrap gap-6">
          {linkedinUrl ? (
            <a
              href={linkedinUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-accent inline-flex items-center gap-1.5 text-sm font-semibold tracking-widest uppercase underline"
            >
              <LinkedinIcon className="size-4" />
              LinkedIn
            </a>
          ) : null}
          {githubUrl ? (
            <a
              href={githubUrl}
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
