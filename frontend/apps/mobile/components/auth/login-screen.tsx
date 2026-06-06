import { DiscordIcon } from "@/components/auth/discord-icon";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import * as WebBrowser from "expo-web-browser";
import { View } from "react-native";

const KEYCLOAK_URL =
  process.env.EXPO_PUBLIC_KEYCLOAK_URL ??
  "http://localhost:8080/oauth2/authorization/keycloak-mobile";
const DISCORD_URL = process.env.EXPO_PUBLIC_DISCORD_URL ?? "";
const REDIRECT_URI = "pkka://";

export function LoginScreen() {
  const { login, user } = useAuth();

  console.log(user);

  async function loginWithKeycloak() {
    const result = await WebBrowser.openAuthSessionAsync(KEYCLOAK_URL, REDIRECT_URI);
    if (result.type !== "success" || !result.url) return;

    const fragment = result.url.split("#")[1] ?? "";
    const params = new URLSearchParams(fragment);
    const at = params.get("at");
    const rt = params.get("rt");
    if (at && rt) {
      await login(at, rt);
    } else {
      console.warn("Failed to parse tokens from redirect URL");
    }
  }

  return (
    <View className="flex-1 items-center justify-center bg-muted px-4">
      <View className="w-full rounded-xl bg-card p-7">
        <Text className="mb-2 text-4xl font-extrabold tracking-tight text-foreground">
          Zaloguj się
        </Text>

        <Text variant="muted" className="leading-relaxed">
          Zaloguj się, aby dołączyć do społeczności klubu alumnów wydziału informatyki AGH.
        </Text>

        <View className="h-8" />

        <Button size="lg" className="w-full" onPress={loginWithKeycloak}>
          <Text>Zaloguj się przez SSO</Text>
        </Button>

        <View className="my-5 flex-row items-center gap-3">
          <View className="h-px flex-1 bg-border" />
          <Text className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            lub
          </Text>
          <View className="h-px flex-1 bg-border" />
        </View>

        <Button
          variant="outline"
          size="lg"
          className="w-full"
          onPress={() => WebBrowser.openBrowserAsync(DISCORD_URL)}
        >
          <DiscordIcon size={20} color="#000" />
          <Text>Kontynuuj przez Discord</Text>
        </Button>

        <View className="mt-7 flex-row items-center justify-center">
          <Text variant="muted">Nie masz konta? </Text>
          <Button variant="link" onPress={loginWithKeycloak}>
            <Text className="font-bold text-foreground">Zarejestruj się</Text>
          </Button>
        </View>
      </View>
    </View>
  );
}
