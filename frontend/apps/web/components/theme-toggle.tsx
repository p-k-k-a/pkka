"use client";

import { Moon, Sun } from "lucide-react";
import { useSyncExternalStore } from "react";
import { Button } from "@/components/ui/button";

type Theme = "light" | "dark";
const THEME_QUERY = "(prefers-color-scheme: dark)";
const THEME_STORAGE_KEY = "theme";
const THEME_CHANGE_EVENT = "themechange";

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  root.classList.toggle("dark", theme === "dark");
  root.style.colorScheme = theme;
}

function getSystemTheme(): Theme {
  return window.matchMedia(THEME_QUERY).matches ? "dark" : "light";
}

function getSnapshot(): Theme {
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  return stored === "dark" || stored === "light" ? stored : getSystemTheme();
}

function getServerSnapshot(): Theme {
  return "light";
}

function subscribe(callback: () => void) {
  const media = window.matchMedia(THEME_QUERY);
  media.addEventListener("change", callback);
  window.addEventListener("storage", callback);
  window.addEventListener(THEME_CHANGE_EVENT, callback);

  return () => {
    media.removeEventListener("change", callback);
    window.removeEventListener("storage", callback);
    window.removeEventListener(THEME_CHANGE_EVENT, callback);
  };
}

export function ThemeToggle() {
  const theme = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  const toggleTheme = () => {
    const nextTheme: Theme = theme === "dark" ? "light" : "dark";
    applyTheme(nextTheme);
    window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
    window.dispatchEvent(new Event(THEME_CHANGE_EVENT));
  };

  return (
    <Button
      type="button"
      variant="outline"
      size="icon"
      onClick={toggleTheme}
      className="rounded-full"
      aria-label={theme === "dark" ? "Włącz jasny motyw" : "Włącz ciemny motyw"}
      title={theme === "dark" ? "Jasny motyw" : "Ciemny motyw"}
    >
      {theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </Button>
  );
}
