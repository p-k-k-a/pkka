import { SettingRow } from "@/components/profile/setting-row";
import { Switch } from "@/components/ui/switch";
import { Text } from "@/components/ui/text";
import {
  getNotificationPermissionStatus,
  registerForPushNotifications,
  switchPushNotifications,
  type PushStatus,
} from "@/lib/notifications";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Bell } from "lucide-react-native";
import { AppState, Linking, Pressable, View } from "react-native";

const SWITCH_KEY = ["push-switch"];

export function NotificationToggle() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<PushStatus>("off");

  useEffect(() => {
    const sync = async () => {
      if (queryClient.isMutating({ mutationKey: SWITCH_KEY }) > 0) return;

      const current = await getNotificationPermissionStatus();
      setStatus(current);
      if (current === "on") await registerForPushNotifications();
    };

    void sync();
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") void sync();
    });
    return () => subscription.remove();
  }, [queryClient]);

  const toggle = useMutation({
    mutationKey: SWITCH_KEY,
    mutationFn: switchPushNotifications,
    onSuccess: (next) => setStatus(next),
    onError: (error) => console.warn("Push toggle failed:", error),
  });

  const checked = toggle.isPending ? toggle.variables : status !== "off";

  return (
    <View className="gap-3">
      <SettingRow
        icon={Bell}
        title="Powiadomienia"
        description="Nowe wydarzenia i przypomnienia"
        action={
          <Switch
            checked={checked}
            accessibilityLabel="Powiadomienia push"
            onCheckedChange={(next) => {
              if (!toggle.isPending) toggle.mutate(next);
            }}
          />
        }
      />

      {toggle.isError ? (
        <Text className="text-destructive dark:text-foreground text-xs font-semibold">
          {toggle.variables
            ? "Nie udało się włączyć powiadomień. Spróbuj ponownie."
            : "Nie udało się wyłączyć powiadomień. Spróbuj ponownie."}
        </Text>
      ) : null}

      {status === "blocked" ? (
        <Pressable
          className="border-border rounded-md border px-3 py-3"
          onPress={() => void Linking.openSettings()}
        >
          <Text className="text-muted-foreground text-sm leading-5">
            Powiadomienia są wyłączone w ustawieniach systemu. Dotknij, aby je włączyć.
          </Text>
        </Pressable>
      ) : null}
    </View>
  );
}
