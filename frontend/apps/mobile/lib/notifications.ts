import {
  NotificationType,
  registerDevice,
  unregisterDevice,
  type NotificationPayload,
} from "@pkka/api";
import Constants from "expo-constants";
import * as Notifications from "expo-notifications";
import { router } from "expo-router";
import * as SecureStore from "expo-secure-store";
import { useEffect } from "react";
import { Platform } from "react-native";

const REGISTERED_TOKEN_KEY = "pushToken";
const PREFERENCE_KEY = "pushNotificationsEnabled";
const ACCESS_TOKEN_KEY = "at";

export type PushRegistration = "registered" | "denied" | "unavailable" | "unsupported" | "off";

const projectId =
  Constants.expoConfig?.extra?.eas?.projectId || Constants.easConfig?.projectId || "";

const NOTIFICATION_TYPES: readonly string[] = Object.values(NotificationType);

export function parseNotificationPayload(data: unknown): NotificationPayload | null {
  if (typeof data !== "object" || data === null) return null;

  const { type, targetId } = data as Record<string, unknown>;
  if (typeof type !== "string" || typeof targetId !== "string" || targetId === "") return null;
  if (!NOTIFICATION_TYPES.includes(type)) return null;

  return { type: type as NotificationType, targetId };
}

export async function isPushEnabled(): Promise<boolean> {
  return (await SecureStore.getItemAsync(PREFERENCE_KEY)) !== "false";
}

export async function setPushEnabled(enabled: boolean): Promise<void> {
  await SecureStore.setItemAsync(PREFERENCE_KEY, String(enabled));
}

async function ensureChannels(): Promise<void> {
  await Notifications.setNotificationChannelAsync("events", {
    name: "Nowe wydarzenia",
    importance: Notifications.AndroidImportance.DEFAULT,
  });
  await Notifications.setNotificationChannelAsync("reminders", {
    name: "Przypomnienia",
    importance: Notifications.AndroidImportance.HIGH,
  });
}

export type PushStatus = "off" | "on" | "blocked";

export async function getNotificationPermissionStatus(): Promise<PushStatus> {
  if (!(await isPushEnabled())) return "off";

  const { granted, canAskAgain } = await Notifications.getPermissionsAsync();
  if (granted) return "on";
  return canAskAgain ? "off" : "blocked";
}

async function ensurePermission(): Promise<boolean> {
  const current = await Notifications.getPermissionsAsync();
  if (current.granted) return true;
  if (!current.canAskAgain) return false;

  return (await Notifications.requestPermissionsAsync()).granted;
}

async function fetchToken(devicePushToken?: Notifications.DevicePushToken): Promise<string | null> {
  if (!projectId) {
    console.warn("No EAS projectId in app.json, push tokens cannot be issued.");
    return null;
  }
  try {
    const { data } = await Notifications.getExpoPushTokenAsync(
      devicePushToken ? { projectId, devicePushToken } : { projectId },
    );
    return data;
  } catch (error) {
    console.warn("Could not obtain an Expo push token:", error);
    return null;
  }
}

async function storeToken(token: string): Promise<void> {
  const previous = await SecureStore.getItemAsync(REGISTERED_TOKEN_KEY);
  if (previous && previous !== token) {
    await unregisterDevice(encodeURIComponent(previous)).catch(() => undefined);
  }
  await registerDevice(encodeURIComponent(token), { platform: "ANDROID" });
  await SecureStore.setItemAsync(REGISTERED_TOKEN_KEY, token);
}

export async function registerForPushNotifications(): Promise<PushRegistration> {
  if (Platform.OS !== "android") return "unsupported";
  if (!(await isPushEnabled())) return "off";

  await ensureChannels();
  if (!(await ensurePermission())) return "denied";

  const token = await fetchToken();
  if (!token) return "unavailable";

  await storeToken(token);
  return "registered";
}

export async function unregisterFromPushNotifications(): Promise<void> {
  const token = await SecureStore.getItemAsync(REGISTERED_TOKEN_KEY);
  if (!token) return;

  try {
    await unregisterDevice(encodeURIComponent(token));
  } catch {
    // Already gone server-side, or the session has expired
  }
  await SecureStore.deleteItemAsync(REGISTERED_TOKEN_KEY);
}

export function watchForTokenRotation(): Notifications.EventSubscription {
  return Notifications.addPushTokenListener((devicePushToken) => {
    void (async () => {
      if (!(await isPushEnabled())) return;
      const token = await fetchToken(devicePushToken);
      if (!token) return;

      // A rotation can land after sign-out
      if (!(await SecureStore.getItemAsync(ACCESS_TOKEN_KEY))) return;
      await storeToken(token);
    })().catch((error) => console.warn("Push token refresh failed:", error));
  });
}

export function configureNotificationHandler(): void {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldPlaySound: true,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });
}

export function useNotificationRouting() {
  const response = Notifications.useLastNotificationResponse();

  useEffect(() => {
    const payload = parseNotificationPayload(response?.notification.request.content.data);
    if (!payload) return;

    router.push({ pathname: "/events/[id]", params: { id: payload.targetId } });
    Notifications.clearLastNotificationResponse();
  }, [response]);
}
