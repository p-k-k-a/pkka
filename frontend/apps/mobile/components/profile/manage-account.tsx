import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { ACCOUNT_CONSOLE_URL } from "@/lib/auth-config";
import { useThemeColors } from "@/lib/theme";
import * as WebBrowser from "expo-web-browser";
import { ExternalLink, UserCog } from "lucide-react-native";

export function ManageAccount() {
  const colors = useThemeColors();

  return (
    <Button
      size="lg"
      variant="outline"
      className="w-full"
      accessibilityHint="Otwiera w przeglądarce ustawienia hasła, 2FA, sesji i konta Discord"
      onPress={() => WebBrowser.openBrowserAsync(ACCOUNT_CONSOLE_URL)}
    >
      <UserCog size={18} color={colors.foreground} />
      <Text className="font-bold">Zarządzaj kontem Keycloak</Text>
      <ExternalLink size={16} color={colors.mutedForeground} />
    </Button>
  );
}
