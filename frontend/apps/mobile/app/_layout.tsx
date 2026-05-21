import "@/global.css";
import { NAV_THEME } from "@/lib/theme";
import { configureApi } from "@pkka/api";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@react-navigation/native";
import { PortalHost } from "@rn-primitives/portal";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import "react-native-reanimated";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";

configureApi({ baseUrl: process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080" });

if (__DEV__) {
  require("../msw.setup");
}

const queryClient = new QueryClient();

export default function RootLayout() {
  const colorScheme: "light" | "dark" = "light";

  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider value={NAV_THEME[colorScheme]}>
          <SafeAreaView className="flex-1 bg-background">
            <Stack>
              <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
              <Stack.Screen name="index" options={{ headerShown: false }} />
            </Stack>
          </SafeAreaView>
          <StatusBar style="dark" />
          <PortalHost />
        </ThemeProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
