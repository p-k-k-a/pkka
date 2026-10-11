import { THEME } from "@pkka/theme";
import { DarkTheme, DefaultTheme, type Theme } from "@react-navigation/native";
import { useColorScheme } from "nativewind";

export { THEME };

export const SCRIM_COLOR = "#000000";
export const SCRIM_BACKDROP = "rgba(0, 0, 0, 0.4)";

export type ColorScheme = "light" | "dark";

export function useAppColorScheme(): ColorScheme {
  const { colorScheme } = useColorScheme();
  return colorScheme === "dark" ? "dark" : "light";
}

export function useThemeColors() {
  return THEME[useAppColorScheme()];
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
