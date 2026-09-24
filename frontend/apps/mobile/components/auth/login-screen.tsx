import { Button } from "@/components/ui/button";
import { DiscordIcon } from "@pkka/icons/native";
import { Eyebrow } from "@/components/ui/page-header";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import { ArrowRight } from "lucide-react-native";
import * as WebBrowser from "expo-web-browser";
import { View } from "react-native";

const KEYCLOAK_URL =
  process.env.EXPO_PUBLIC_KEYCLOAK_URL ??
  "http://localhost:8080/oauth2/authorization/keycloak-mobile";
const DISCORD_URL = process.env.EXPO_PUBLIC_DISCORD_URL ?? "";
const REDIRECT_URI = "pkka://";

export function LoginScreen() {
  const { login } = useAuth();
  const theme = useThemeColors();

  async function loginWith(authorizationUrl: string) {
    if (!authorizationUrl) {
      console.warn("Missing authorization URL — check EXPO_PUBLIC_* env vars");
      return;
    }

    const result = await WebBrowser.openAuthSessionAsync(authorizationUrl, REDIRECT_URI);
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
    <View className="flex-1 items-center justify-center bg-muted px-5 dark:bg-background">
      <View className="w-full gap-0 rounded-3xl border border-border bg-card p-7 shadow-sm shadow-black/5">
        <Eyebrow onCard className="mb-4">
          Klub Alumnów WI AGH
        </Eyebrow>
        <Text
          role="heading"
          aria-level="1"
          className="mb-3 font-heading text-[33px] font-semibold leading-tight tracking-tight text-foreground"
        >
          Zaloguj się
        </Text>

        <Text className="text-base leading-relaxed text-muted-foreground">
          Zaloguj się, aby dołączyć do społeczności klubu alumnów wydziału informatyki AGH.
        </Text>

        <View className="h-8" />

        <Button size="lg" className="w-full" onPress={() => loginWith(KEYCLOAK_URL)}>
          <Text className="text-base font-semibold">Zaloguj się przez SSO</Text>
          <ArrowRight size={18} color={theme.primaryForeground} />
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
          onPress={() => loginWith(DISCORD_URL)}
        >
          <DiscordIcon size={20} color={theme.foreground} />
          <Text className="text-base font-semibold">Kontynuuj przez Discord</Text>
        </Button>

        <View className="mt-7 flex-row items-center justify-center">
          <Text variant="muted">Nie masz konta? </Text>
          <Button variant="link" onPress={() => loginWith(KEYCLOAK_URL)}>
            <Text className="font-bold text-accent underline dark:text-primary">
              Zarejestruj się
            </Text>
          </Button>
        </View>
      </View>
    </View>
  );
}
