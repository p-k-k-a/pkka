import Svg, { Path } from "react-native-svg";
import { BRAND_ICONS, type BrandIconName } from "./index";

export type BrandIconProps = {
  size?: number;
  color?: string;
};

function BrandIcon({ name, size, color }: BrandIconProps & { name: BrandIconName }) {
  const icon = BRAND_ICONS[name];
  return (
    <Svg width={size} height={size} viewBox={icon.viewBox} fill={color}>
      <Path d={icon.path} />
    </Svg>
  );
}

export function DiscordIcon({ size = 16, color = "currentColor" }: BrandIconProps) {
  return <BrandIcon name="discord" size={size} color={color} />;
}

export function LinkedinIcon({ size = 16, color = "#0A66C2" }: BrandIconProps) {
  return <BrandIcon name="linkedin" size={size} color={color} />;
}

export function GithubIcon({ size = 16, color = "#181717" }: BrandIconProps) {
  return <BrandIcon name="github" size={size} color={color} />;
}
