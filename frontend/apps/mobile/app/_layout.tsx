import "@/global.css";
import { AuthProvider } from "@/lib/auth-context";
import { ProfileProvider } from "@/lib/profile-context";
import { NAV_THEME } from "@/lib/theme";
import { ApiError } from "@pkka/api";
import { ThemeProvider } from "@react-navigation/native";
import { PortalHost } from "@rn-primitives/portal";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { KeyboardProvider } from "react-native-keyboard-controller";
import "react-native-reanimated";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Don't retry client errors (4xx) — a 404 like "no application yet" is a
      // final answer, not a transient failure. Still retry network/5xx blips.
      retry: (failureCount, error) => {
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
          return false;
        }
        return failureCount < 3;
      },
    },
  },
});

export default function RootLayout() {
  const colorScheme: "light" | "dark" = "light";

  return (
    <KeyboardProvider>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider value={NAV_THEME[colorScheme]}>
            <AuthProvider>
              <ProfileProvider>
                <SafeAreaView className="flex-1 bg-background">
                  <Stack>
                    <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
                    <Stack.Screen name="index" options={{ headerShown: false }} />
                    <Stack.Screen name="events/[id]" options={{ headerShown: false }} />
                    <Stack.Screen name="application" options={{ headerShown: false }} />
                    <Stack.Screen name="alumni/profile-edit" options={{ headerShown: false }} />
                  </Stack>
                </SafeAreaView>
                <StatusBar style="auto" />
                <PortalHost />
              </ProfileProvider>
            </AuthProvider>
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </KeyboardProvider>
  );
}
