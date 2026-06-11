import { DiscordIcon } from "@/components/auth/discord-icon";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import { MOCK_ACCOUNT_STATUS, mockVerifyWithDiscord } from "@/lib/application-mocks";
import { useAuth } from "@/lib/auth-context";
import { useTheme } from "@react-navigation/native";
import { router } from "expo-router";
import { ClipboardList, LogOut } from "lucide-react-native";
import { View } from "react-native";

export function UserPanel() {
  const { logout } = useAuth();
  const { colors } = useTheme();

  return (
    <View className="flex-1 bg-background px-4 py-8 gap-8">
      <Text variant="h1" className="text-left text-4xl">
        Profil
      </Text>

      <View className="gap-5">
        <View className="self-start flex-row items-center gap-2 rounded-full border border-destructive px-3 py-1.5">
          <View className="bg-destructive size-2 rounded-full" />
          <Text className="text-destructive text-xs font-semibold">
            {MOCK_ACCOUNT_STATUS.label}
          </Text>
        </View>

        <View className="gap-2">
          <Text className="text-foreground text-3xl font-extrabold tracking-tight leading-9">
            Potwierdź status absolwenta
          </Text>
          <Text className="text-muted-foreground text-sm leading-6">
            Zweryfikuj konto, aby odblokować pełny dostęp do społeczności i wydarzeń.
          </Text>
        </View>

        <View className="gap-3 mt-2">
          <Button size="lg" className="w-full" onPress={mockVerifyWithDiscord}>
            <DiscordIcon size={18} color={colors.background} />
            <Text className="font-bold">Zweryfikuj przez Discord</Text>
          </Button>

          <Button size="lg" className="w-full" onPress={() => router.push("/application")}>
            <ClipboardList size={18} color={colors.background} />
            <Text className="font-bold">Złóż wniosek ręcznie</Text>
          </Button>
        </View>
      </View>

      <Separator />

      <Button size="lg" className="w-full" onPress={logout}>
        <LogOut size={18} color={colors.background} />
        <Text className="font-bold">Wyloguj się</Text>
      </Button>
    </View>
  );
}
