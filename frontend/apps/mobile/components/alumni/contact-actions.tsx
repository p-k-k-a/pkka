import { Button } from "@/components/ui/button";
import { DiscordIcon, GithubIcon, LinkedinIcon } from "@/components/ui/svg-icons";
import { Text } from "@/components/ui/text";
import type { AlumnProfile } from "@/lib/alumni-mock";
import { THEME } from "@/lib/theme";
import * as Linking from "expo-linking";
import * as WebBrowser from "expo-web-browser";
import { Mail } from "lucide-react-native";
import { Alert, Pressable, View } from "react-native";

type ContactActionsProps = {
  profile: AlumnProfile;
};

// Profile URLs are user-supplied server data. Never hand a non-https value to
// Linking.openURL, or a malicious profile could launch arbitrary schemes
// (tel:, third-party app deep links) on the viewer's device.
const isHttpsUrl = (url: string) => /^https:\/\//i.test(url);
const isDiscordId = (id: string) => /^[0-9]{5,32}$/.test(id);

async function openWithFallback(appUrl: string, webUrl: string) {
  try {
    await Linking.openURL(appUrl);
  } catch {
    // the user doesn't have app installed
    try {
      await WebBrowser.openBrowserAsync(webUrl);
    } catch {
      // e.g. a browser sheet is already presenting - nothing sensible to do
    }
  }
}

async function openEmail(email: string) {
  try {
    await Linking.openURL(`mailto:${email}`);
  } catch {
    Alert.alert("Adres e-mail", email);
  }
}

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
  const { email } = profile;
  const showEmail = profile.visibility.email;
  const discordId =
    profile.visibility.discord && profile.discordId && isDiscordId(profile.discordId)
      ? profile.discordId
      : null;
  const linkedinUrl =
    profile.linkedinUrl && isHttpsUrl(profile.linkedinUrl) ? profile.linkedinUrl : null;
  const githubUrl = profile.githubUrl && isHttpsUrl(profile.githubUrl) ? profile.githubUrl : null;

  const hasButtons = !!discordId || showEmail;
  const hasLinks = !!linkedinUrl || !!githubUrl;
  if (!hasButtons && !hasLinks) return null;

  return (
    <View className="gap-5">
      {hasButtons ? (
        <View className="gap-3">
          {discordId ? (
            <Button
              size="lg"
              className="w-full"
              onPress={() =>
                openWithFallback(
                  `discord://-/users/${discordId}`,
                  `https://discord.com/users/${discordId}`,
                )
              }
            >
              <DiscordIcon size={18} color={THEME.light.primaryForeground} />
              <Text className="font-bold">Kontakt przez Discord</Text>
            </Button>
          ) : null}
          {showEmail ? (
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
