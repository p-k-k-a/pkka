import { Separator } from "@/components/ui/separator";
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
    <View className="gap-4">
      <Separator />

      <View className="flex-row items-center justify-between gap-4">
        <View className="shrink gap-1">
          <Text className="text-foreground text-base font-semibold">Powiadomienia</Text>
          <Text className="text-muted-foreground text-sm leading-5">
            O nowych wydarzeniach i przypomnieniach o tych, na które się zapisujesz.
          </Text>
        </View>
        <Switch
          checked={checked}
          disabled={toggle.isPending}
          accessibilityLabel="Powiadomienia push"
          onCheckedChange={(next) => toggle.mutate(next)}
        />
      </View>

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
