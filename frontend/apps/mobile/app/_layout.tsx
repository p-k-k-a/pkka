import "@/global.css";
import { AuthProvider } from "@/lib/auth-context";
import { NAV_THEME } from "@/lib/theme";
import { BottomSheetProvider } from "@/components/ui/bottom-sheet-provider";
import { createQueryClient } from "@pkka/api";
import { parseNotificationPayload } from "@pkka/domain";
import { ThemeProvider } from "@react-navigation/native";
import { PortalHost } from "@rn-primitives/portal";
import { QueryClientProvider } from "@tanstack/react-query";
import * as Notifications from "expo-notifications";
import { router, Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { useEffect } from "react";
import { KeyboardProvider } from "react-native-keyboard-controller";
import "react-native-reanimated";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";

const queryClient = createQueryClient();

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: true,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

function useNotificationRouting() {
  useEffect(() => {
    const openTarget = (response: Notifications.NotificationResponse | null) => {
      const payload = parseNotificationPayload(response?.notification.request.content.data);
      if (!payload) return;
      router.push({ pathname: "/events/[id]", params: { id: payload.targetId } });
    };

    // Read synchronously: a cold start must route after the router mounts but before the value is cleared.
    openTarget(Notifications.getLastNotificationResponse());
    Notifications.clearLastNotificationResponse();

    const subscription = Notifications.addNotificationResponseReceivedListener(openTarget);
    return () => subscription.remove();
  }, []);
}

export default function RootLayout() {
  const colorScheme: "light" | "dark" = "light";
  useNotificationRouting();

  return (
    <KeyboardProvider>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider value={NAV_THEME[colorScheme]}>
            <AuthProvider>
              <BottomSheetProvider>
                <SafeAreaView className="flex-1 bg-background">
                  <Stack>
                    <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
                    <Stack.Screen name="index" options={{ headerShown: false }} />
                    <Stack.Screen name="events/[id]" options={{ headerShown: false }} />
                    <Stack.Screen name="application" options={{ headerShown: false }} />
                    <Stack.Screen name="alumni/profile-edit" options={{ headerShown: false }} />
                    <Stack.Screen name="alumni/[id]" options={{ headerShown: false }} />
                  </Stack>
                </SafeAreaView>
                <StatusBar style="auto" />
                <PortalHost />
              </BottomSheetProvider>
            </AuthProvider>
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </KeyboardProvider>
  );
}
