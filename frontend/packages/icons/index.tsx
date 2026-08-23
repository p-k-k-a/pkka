import { DISCORD_PATH, GITHUB_PATH, ICON_VIEW_BOX, LINKEDIN_PATH } from "./paths";

type IconProps = {
  className?: string;
};

function makeIcon(path: string) {
  return function Icon({ className }: IconProps) {
    return (
      <svg
        viewBox={ICON_VIEW_BOX}
        fill="currentColor"
        aria-hidden="true"
        focusable="false"
        className={className}
      >
        <path d={path} />
      </svg>
    );
  };
}

export const DiscordIcon = makeIcon(DISCORD_PATH);
export const LinkedinIcon = makeIcon(LINKEDIN_PATH);
export const GithubIcon = makeIcon(GITHUB_PATH);
