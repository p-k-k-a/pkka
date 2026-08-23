import Svg, { Path } from "react-native-svg";
import { DISCORD_PATH, GITHUB_PATH, ICON_VIEW_BOX, LINKEDIN_PATH } from "./paths";

type IconProps = {
  size?: number;
  color?: string;
};

function makeIcon(path: string) {
  return function Icon({ size = 16, color = "currentColor" }: IconProps) {
    return (
      <Svg width={size} height={size} viewBox={ICON_VIEW_BOX} fill={color}>
        <Path d={path} />
      </Svg>
    );
  };
}

export const DiscordIcon = makeIcon(DISCORD_PATH);
export const LinkedinIcon = makeIcon(LINKEDIN_PATH);
export const GithubIcon = makeIcon(GITHUB_PATH);
