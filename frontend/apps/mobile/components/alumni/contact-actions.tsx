import { Button } from "@/components/ui/button";
import { DiscordIcon, GithubIcon, LinkedinIcon } from "@pkka/icons/native";
import { Text } from "@/components/ui/text";
import type { AlumnProfile } from "@/components/alumni/alumni-profile-view";
import type { ReactNode } from "react";
import { getProfileContacts } from "@pkka/domain";
import { openDiscordUser, openEmail, openWithFallback } from "@/lib/contact";
import { useThemeColors } from "@/lib/theme";
import { Mail } from "lucide-react-native";
import { Pressable, View } from "react-native";

type ContactActionsProps = {
  profile: AlumnProfile;
};

function ExternalLink({ label, url, icon }: { label: string; url: string; icon: ReactNode }) {
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
  const theme = useThemeColors();
  const { email, discordId, linkedinUrl, githubUrl, hasAny } = getProfileContacts(profile);

  const hasButtons = !!discordId || !!email;
  const hasLinks = !!linkedinUrl || !!githubUrl;
  if (!hasAny) return null;

  return (
    <View className="gap-5">
      {hasButtons ? (
        <View className="gap-3">
          {discordId ? (
            <Button size="lg" className="w-full" onPress={() => openDiscordUser(discordId)}>
              <DiscordIcon size={18} color={theme.primaryForeground} />
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
              <Mail size={18} color={theme.secondaryForeground} />
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
            <ExternalLink
              label="GitHub"
              url={githubUrl}
              icon={<GithubIcon size={16} color={theme.foreground} />}
            />
          ) : null}
        </View>
      ) : null}
    </View>
  );
}
