import { Button } from "@/components/ui/button";
import { DiscordIcon, GithubIcon, LinkedinIcon } from "@/components/ui/svg-icons";
import { Text } from "@/components/ui/text";
import type { AlumnProfile } from "@/lib/alumni-profile";
import {
  isDiscordId,
  isHttpsUrl,
  openDiscordUser,
  openEmail,
  openWithFallback,
} from "@/lib/contact";
import { THEME } from "@/lib/theme";
import { Mail } from "lucide-react-native";
import { Pressable, View } from "react-native";

type ContactActionsProps = {
  profile: AlumnProfile;
};

function ExternalLink({ label, url, icon }: { label: string; url: string; icon: React.ReactNode }) {
  return (
    <Pressable
      role="link"
      accessibilityLabel={label}
      onPress={() => openWithFallback(url, url)}
      className="flex-row items-center gap-1.5"
    >
      {icon}
      <Text className="text-sm font-semibold uppercase tracking-widest text-accent underline">
        {label}
      </Text>
    </Pressable>
  );
}

export function ContactActions({ profile }: ContactActionsProps) {
  const email = profile.visibility.email ? profile.email : undefined;
  const discordId =
    profile.visibility.discord && profile.discordId && isDiscordId(profile.discordId)
      ? profile.discordId
      : undefined;
  const linkedinUrl =
    profile.linkedinUrl && isHttpsUrl(profile.linkedinUrl) ? profile.linkedinUrl : null;
  const githubUrl = profile.githubUrl && isHttpsUrl(profile.githubUrl) ? profile.githubUrl : null;

  const hasButtons = !!discordId || !!email;
  const hasLinks = !!linkedinUrl || !!githubUrl;
  if (!hasButtons && !hasLinks) return null;

  return (
    <View className="gap-5">
      {hasButtons ? (
        <View className="gap-3">
          {discordId ? (
            <Button size="lg" className="w-full" onPress={() => openDiscordUser(discordId)}>
              <DiscordIcon size={18} color={THEME.light.primaryForeground} />
              <Text className="font-bold">Kontakt przez Discord</Text>
            </Button>
          ) : null}
          {email ? (
            <Button
              size="lg"
              variant="secondary"
              className="w-full"
              onPress={() => openEmail(email)}
            >
              <Mail size={18} color={THEME.light.secondaryForeground} />
              <Text className="font-bold">Kontakt przez E-mail</Text>
            </Button>
          ) : null}
        </View>
      ) : null}

      {hasLinks ? (
        <View className="flex-row gap-6">
          {linkedinUrl ? (
            <ExternalLink label="LinkedIn" url={linkedinUrl} icon={<LinkedinIcon size={16} />} />
          ) : null}
          {githubUrl ? (
            <ExternalLink label="GitHub" url={githubUrl} icon={<GithubIcon size={16} />} />
          ) : null}
        </View>
      ) : null}
    </View>
  );
}
