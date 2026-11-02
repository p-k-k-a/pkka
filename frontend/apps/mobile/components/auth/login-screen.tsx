import { Button } from "@/components/ui/button";
import { DiscordIcon } from "@pkka/icons/native";
import { Text } from "@/components/ui/text";
import { DISCORD_URL, KEYCLOAK_URL } from "@/lib/auth-config";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import { View } from "react-native";

export function LoginScreen() {
  const { loginBrowser } = useAuth();
  const theme = useThemeColors();

  return (
    <View className="flex-1 items-center justify-center bg-muted px-5 dark:bg-background">
      <View className="w-full gap-0 rounded-3xl border border-border bg-card p-7 shadow-sm shadow-black/5">
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

        <Button size="lg" className="w-full" onPress={() => loginBrowser(KEYCLOAK_URL)}>
          <Text className="text-base font-semibold">Zaloguj się przez SSO</Text>
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
          onPress={() => loginBrowser(DISCORD_URL)}
        >
          <DiscordIcon size={20} color={theme.foreground} />
          <Text className="text-base font-semibold">Kontynuuj przez Discord</Text>
        </Button>

        <View className="mt-7 flex-row items-center justify-center">
          <Text variant="muted">Nie masz konta? </Text>
          <Button variant="link" onPress={() => loginBrowser(KEYCLOAK_URL)}>
            <Text className="font-bold text-accent underline dark:text-primary">
              Zarejestruj się
            </Text>
          </Button>
        </View>
      </View>
    </View>
  );
}
