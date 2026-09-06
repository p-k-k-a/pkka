import * as Linking from "expo-linking";
import * as WebBrowser from "expo-web-browser";
import { Alert } from "react-native";

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

export function openDiscordUser(discordId: string) {
  return openWithFallback(
    `discord://-/users/${discordId}`,
    `https://discord.com/users/${discordId}`,
  );
}
