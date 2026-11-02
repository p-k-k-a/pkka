import { THEME } from "@pkka/theme";
import { DarkTheme, DefaultTheme, type Theme } from "@react-navigation/native";
import * as SecureStore from "expo-secure-store";
import { colorScheme, useColorScheme } from "nativewind";
import { useSyncExternalStore } from "react";

export { THEME };

export type ColorScheme = "light" | "dark";

export function useAppColorScheme(): ColorScheme {
  const { colorScheme } = useColorScheme();
  return colorScheme === "dark" ? "dark" : "light";
}

export function useThemeColors() {
  return THEME[useAppColorScheme()];
}

export type ThemePreference = "light" | "dark" | "system";

const THEME_PREFERENCE_KEY = "theme-preference";
let themePreference: ThemePreference = "system";
const themeListeners = new Set<() => void>();

function applyThemePreference(next: ThemePreference) {
  themePreference = next;
  colorScheme.set(next);
  themeListeners.forEach((listener) => listener());
}

export function useThemePreference() {
  return useSyncExternalStore(
    (listener) => {
      themeListeners.add(listener);
      return () => themeListeners.delete(listener);
    },
    () => themePreference,
  );
}

export async function setThemePreference(next: ThemePreference) {
  applyThemePreference(next);
  await SecureStore.setItemAsync(THEME_PREFERENCE_KEY, next);
}

export async function restoreThemePreference() {
  const stored = await SecureStore.getItemAsync(THEME_PREFERENCE_KEY);
  if (stored === "light" || stored === "dark" || stored === "system") {
    applyThemePreference(stored);
  }
}

export const NAV_THEME: Record<ColorScheme, Theme> = {
  light: {
    ...DefaultTheme,
    colors: {
      background: THEME.light.background,
      border: THEME.light.border,
      card: THEME.light.card,
      notification: THEME.light.destructive,
      primary: THEME.light.primary,
      text: THEME.light.foreground,
    },
  },
  dark: {
    ...DarkTheme,
    colors: {
      background: THEME.dark.background,
      border: THEME.dark.border,
      card: THEME.dark.card,
      notification: THEME.dark.destructive,
      primary: THEME.dark.primary,
      text: THEME.dark.foreground,
    },
  },
};
