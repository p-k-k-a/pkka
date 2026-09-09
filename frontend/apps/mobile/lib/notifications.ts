import { registerDevice, unregisterDevice } from "@pkka/api";
import Constants from "expo-constants";
import * as Crypto from "expo-crypto";
import * as Notifications from "expo-notifications";
import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";

const INSTALLATION_ID_KEY = "pushInstallationId";
const PREFERENCE_KEY = "pushNotificationsEnabled";
/** Written by the auth context; its presence is what "signed in" means to the API client too. */
const ACCESS_TOKEN_KEY = "at";

export type PushRegistration = "registered" | "denied" | "unavailable" | "unsupported" | "off";

const projectId =
  Constants.expoConfig?.extra?.eas?.projectId || Constants.easConfig?.projectId || "";

/** Stable for the life of the install, unlike the push token, which the service rotates. */
export async function getInstallationId(): Promise<string> {
  const existing = await SecureStore.getItemAsync(INSTALLATION_ID_KEY);
  if (existing) return existing;

  const created = Crypto.randomUUID();
  await SecureStore.setItemAsync(INSTALLATION_ID_KEY, created);
  return created;
}

export async function isPushEnabled(): Promise<boolean> {
  return (await SecureStore.getItemAsync(PREFERENCE_KEY)) !== "false";
}

export async function setPushEnabled(enabled: boolean): Promise<void> {
  await SecureStore.setItemAsync(PREFERENCE_KEY, String(enabled));
}

/**
 * Android shows no permission prompt until a channel exists, and a channel's importance can never be changed
 * afterwards — so these two ids and levels are effectively permanent.
 */
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

/** Android 13 stops prompting after two dismissals, so the only way back is the system settings screen. */
export async function isPushPermissionBlocked(): Promise<boolean> {
  if (Platform.OS !== "android") return false;

  const current = await Notifications.getPermissionsAsync();
  return !current.granted && !current.canAskAgain;
}

async function ensurePermission(): Promise<boolean> {
  const current = await Notifications.getPermissionsAsync();
  if (current.granted) return true;
  if (!current.canAskAgain) return false;

  return (await Notifications.requestPermissionsAsync()).granted;
}

async function fetchToken(devicePushToken?: Notifications.DevicePushToken): Promise<string | null> {
  if (!projectId) {
    console.warn("No EAS projectId in app.json — push tokens cannot be issued.");
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
  await registerDevice(await getInstallationId(), { token, platform: "ANDROID" });
}

/** Safe to call on every sign-in: the endpoint upserts on the installation id. */
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
  const installationId = await SecureStore.getItemAsync(INSTALLATION_ID_KEY);
  if (!installationId) return;

  try {
    await unregisterDevice(installationId);
  } catch {
    // Already gone server-side, or the session has expired — either way there is nothing left to remove.
  }
}

/**
 * The push service can roll a token while the app runs. Passing the token the listener just handed us avoids
 * the infinite loop that calling `getDevicePushTokenAsync` in here would cause.
 */
export function watchForTokenRotation(): Notifications.EventSubscription {
  return Notifications.addPushTokenListener((devicePushToken) => {
    void (async () => {
      if (!(await isPushEnabled())) return;
      const token = await fetchToken(devicePushToken);
      if (!token) return;

      // A rotation can land after sign-out; re-registering then would revive the device the user just released.
      if (!(await SecureStore.getItemAsync(ACCESS_TOKEN_KEY))) return;
      await storeToken(token);
    })();
  });
}
