import type { AlumnProfile } from "@/components/alumni/alumni-profile-view";
import * as Linking from "expo-linking";
import * as WebBrowser from "expo-web-browser";
import { Alert } from "react-native";

// Profile URLs are user-supplied server data. Never hand a non-https URL to
// Linking.openURL, or a malicious profile could launch arbitrary schemes
// (tel:, third-party app deep links) on the viewer's device.
export const isHttpsUrl = (url: string) => /^https:\/\//i.test(url);
export const isDiscordId = (id: string) => /^[0-9]{5,32}$/.test(id);

export async function openWithFallback(appUrl: string, webUrl: string) {
  try {
    await Linking.openURL(appUrl);
  } catch {
    // the user doesn't have the app installed
    try {
      await WebBrowser.openBrowserAsync(webUrl);
    } catch {
      // e.g. a browser sheet is already presenting - nothing sensible to do
    }
  }
}

export async function openEmail(email: string) {
  try {
    await Linking.openURL(`mailto:${email}`);
  } catch {
    Alert.alert("Adres e-mail", email);
  }
}

export function discordUserIdOf(profile: AlumnProfile): string | null {
  return profile.visibility.discord && profile.discordId && isDiscordId(profile.discordId)
    ? profile.discordId
    : null;
}

export function openDiscordUser(discordId: string) {
  return openWithFallback(
    `discord://-/users/${discordId}`,
    `https://discord.com/users/${discordId}`,
  );
}
