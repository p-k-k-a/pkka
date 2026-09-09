import { Separator } from "@/components/ui/separator";
import { Switch } from "@/components/ui/switch";
import { Text } from "@/components/ui/text";
import {
  isPushEnabled,
  isPushPermissionBlocked,
  registerForPushNotifications,
  setPushEnabled,
  unregisterFromPushNotifications,
} from "@/lib/notifications";
import { useEffect, useState } from "react";
import { Linking, Pressable, View } from "react-native";

export function NotificationToggle() {
  const [enabled, setEnabled] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void (async () => {
      setEnabled(await isPushEnabled());
      setBlocked(await isPushPermissionBlocked());
    })();
  }, []);

  // `next` comes from the argument, never from the captured state: React Compiler memoises this handler.
  const toggle = async (next: boolean) => {
    if (busy) return;
    setBusy(true);
    setEnabled(next);
    await setPushEnabled(next);

    if (next) {
      setBlocked((await registerForPushNotifications()) === "denied");
    } else {
      await unregisterFromPushNotifications();
      setBlocked(false);
    }
    setBusy(false);
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
          checked={enabled}
          disabled={busy}
          accessibilityLabel="Powiadomienia push"
          onCheckedChange={(next) => void toggle(next)}
        />
      </View>

      {enabled && blocked ? (
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
