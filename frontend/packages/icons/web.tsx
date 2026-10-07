import { BRAND_ICONS, type BrandIconName } from "./index";

export type BrandIconProps = {
  className?: string;
  color?: string;
};

function BrandIcon({ name, className, color }: BrandIconProps & { name: BrandIconName }) {
  const icon = BRAND_ICONS[name];
  return (
    <svg
      viewBox={icon.viewBox}
      fill={color}
      aria-hidden="true"
      focusable="false"
      className={className}
    >
      <path d={icon.path} />
    </svg>
  );
}

export function DiscordIcon({ className, color = "currentColor" }: BrandIconProps) {
  return <BrandIcon name="discord" className={className} color={color} />;
}

export function LinkedinIcon({ className, color = "currentColor" }: BrandIconProps) {
  return <BrandIcon name="linkedin" className={className} color={color} />;
}

export function GithubIcon({ className, color = "currentColor" }: BrandIconProps) {
  return <BrandIcon name="github" className={className} color={color} />;
}
