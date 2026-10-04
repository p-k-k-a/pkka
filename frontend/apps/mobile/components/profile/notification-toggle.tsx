import { Separator } from "@/components/ui/separator";
import { Switch } from "@/components/ui/switch";
import { Text } from "@/components/ui/text";
import {
  getNotificationPermissionStatus,
  registerForPushNotifications,
  setPushEnabled,
  unregisterFromPushNotifications,
  type PushStatus,
} from "@/lib/notifications";
import { useEffect, useState } from "react";
import { AppState, Linking, Pressable, View } from "react-native";

export function NotificationToggle() {
  const [status, setStatus] = useState<PushStatus>("off");
  const [pending, setPending] = useState(false);

  useEffect(() => {
    const sync = async () => {
      const current = await getNotificationPermissionStatus();
      setStatus(current);
      if (current === "on") await registerForPushNotifications();
    };

    void sync();
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") void sync();
    });
    return () => subscription.remove();
  }, []);

  const toggle = async (next: boolean) => {
    setPending(true);
    setStatus(next ? "on" : "off");
    try {
      await setPushEnabled(next);
      if (!next) return await unregisterFromPushNotifications();

      if ((await registerForPushNotifications()) === "registered") return;

      const blocked = (await getNotificationPermissionStatus()) === "blocked";
      if (!blocked) await setPushEnabled(false);
      setStatus(blocked ? "blocked" : "off");
    } finally {
      setPending(false);
    }
  };

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
          checked={status !== "off"}
          disabled={pending}
          accessibilityLabel="Powiadomienia push"
          onCheckedChange={(next) =>
            void toggle(next).catch((error) => console.warn("Push toggle failed:", error))
          }
        />
      </View>

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
