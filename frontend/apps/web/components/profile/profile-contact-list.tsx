import { Mail } from "lucide-react";
import type { ProfileContacts } from "@pkka/domain";
import { DiscordIcon, GithubIcon, LinkedinIcon } from "@pkka/icons/web";

const ICON_CLASS = "text-muted-foreground mt-0.5 size-4 shrink-0";

function ContactItem({
  icon,
  href,
  external = false,
  children,
}: {
  icon: React.ReactNode;
  href: string;
  external?: boolean;
  children: React.ReactNode;
}) {
  return (
    <li className="flex items-start gap-3">
      {icon}
      <a
        href={href}
        {...(external ? { target: "_blank", rel: "noopener noreferrer" } : {})}
        className="text-accent min-w-0 text-sm font-semibold break-all underline-offset-4 hover:underline"
      >
        {children}
      </a>
    </li>
  );
}

export function ProfileContactList({ contacts }: { contacts: ProfileContacts }) {
  const { email, discordUrl, linkedinUrl, githubUrl } = contacts;

  return (
    <ul className="space-y-5">
      {email ? (
        <ContactItem
          icon={<Mail className={ICON_CLASS} aria-hidden="true" />}
          href={`mailto:${email}`}
        >
          {email}
        </ContactItem>
      ) : null}
      {discordUrl ? (
        <ContactItem icon={<DiscordIcon className={ICON_CLASS} />} href={discordUrl} external>
          Discord
        </ContactItem>
      ) : null}
      {linkedinUrl ? (
        <ContactItem icon={<LinkedinIcon className={ICON_CLASS} />} href={linkedinUrl} external>
          LinkedIn
        </ContactItem>
      ) : null}
      {githubUrl ? (
        <ContactItem icon={<GithubIcon className={ICON_CLASS} />} href={githubUrl} external>
          GitHub
        </ContactItem>
      ) : null}
    </ul>
  );
}
