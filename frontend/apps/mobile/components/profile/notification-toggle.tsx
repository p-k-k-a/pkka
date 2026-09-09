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
import { useEffect, useRef, useState } from "react";
import { AppState, Linking, Pressable, View } from "react-native";

export function NotificationToggle() {
  const [enabled, setEnabled] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const [busy, setBusy] = useState(false);
  const wasBlocked = useRef(false);

  useEffect(() => {
    const refresh = async () => {
      const on = await isPushEnabled();
      setEnabled(on);

      const nowBlocked = on && (await isPushPermissionBlocked());
      // Granting the permission happens on the system settings screen, so the only signal we get that it
      // worked is coming back to the app — without this the banner stays up and no token is ever registered.
      if (wasBlocked.current && !nowBlocked && on) {
        await registerForPushNotifications();
      }
      wasBlocked.current = nowBlocked;
      setBlocked(nowBlocked);
    };

    void refresh();
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") void refresh();
    });
    return () => subscription.remove();
  }, []);

  // `next` comes from the argument, never from the captured state: React Compiler memoises this handler.
  const toggle = async (next: boolean) => {
    if (busy) return;
    setBusy(true);
    setEnabled(next);

    try {
      await setPushEnabled(next);

      if (!next) {
        await unregisterFromPushNotifications();
        setBlocked(false);
        wasBlocked.current = false;
        return;
      }

      const result = await registerForPushNotifications();
      const denied = result === "denied";
      setBlocked(denied);
      wasBlocked.current = denied;

      // Denied is recoverable from system settings, so the switch stays on and explains itself. Nothing
      // recovers a missing project id or an unsupported platform, so those settle back to off.
      if (result === "unavailable" || result === "unsupported") {
        setEnabled(false);
        await setPushEnabled(false);
      }
    } catch {
      setEnabled(!next);
      await setPushEnabled(!next);
    } finally {
      setBusy(false);
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
