import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import { DiscordIcon } from "@pkka/icons/native";
import { DiscordStatus, useGetMyDiscordStatus } from "@pkka/api";
import { ArrowUpRight } from "lucide-react-native";
import { Linking, View } from "react-native";

export function DiscordBanner() {
  const theme = useThemeColors();
  const { loginDiscord } = useAuth();
  const { data } = useGetMyDiscordStatus();
  const status = data?.data.status;
  const inviteUrl = data?.data.inviteUrl;

  if (status === DiscordStatus.NOT_LINKED) {
    return (
      <View className="bg-muted dark:bg-card gap-3 rounded-2xl p-5">
        <Text className="text-foreground text-base font-semibold">
          Podepnij Discorda, żeby wejść na kanał grupy
        </Text>
        <Button variant="outline" onPress={loginDiscord} className="w-full">
          <DiscordIcon size={18} color={theme.foreground} />
          <Text className="font-semibold">Połącz z Discordem</Text>
        </Button>
      </View>
    );
  }

  if (status === DiscordStatus.NOT_ON_SERVER && inviteUrl) {
    return (
      <View className="bg-muted dark:bg-card gap-3 rounded-2xl p-5">
        <View className="gap-1">
          <Text className="text-foreground text-base font-semibold">
            Nie ma Cię na serwerze klubu
          </Text>
          <Text className="text-muted-foreground text-sm leading-relaxed">
            Dołącz do serwera, a kanał grupy pojawi się u Ciebie na Discordzie.
          </Text>
        </View>
        <Button
          variant="outline"
          onPress={() => void Linking.openURL(inviteUrl)}
          className="w-full"
        >
          <DiscordIcon size={18} color={theme.foreground} />
          <Text className="font-semibold">Dołącz do serwera</Text>
          <ArrowUpRight size={16} color={theme.foreground} />
        </Button>
      </View>
    );
  }

  return null;
}
