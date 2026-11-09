import { API_URL, DISCORD_URL, REDIRECT_URI } from "@/lib/auth-config";
import {
  registerForPushNotifications,
  unregisterFromPushNotifications,
  watchForTokenRotation,
} from "@/lib/notifications";
import { AuthContextType, User } from "@/types/auth";
import { configureApi, logoutTokens, refreshTokens } from "@pkka/api";
import { useQueryClient } from "@tanstack/react-query";
import * as SecureStore from "expo-secure-store";
import * as WebBrowser from "expo-web-browser";
import { jwtDecode } from "jwt-decode";
import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { AppState } from "react-native";

export const AuthContext = createContext<null | AuthContextType>(null);

type decodedJwtType = {
  sub: string;
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const loggingOut = useRef(false);
  const queryClient = useQueryClient();

  const saveJWT = async (at: string, rt: string) => {
    const decoded = jwtDecode(at) as decodedJwtType;
    setUser({ sub: decoded["sub"] });

    await SecureStore.setItemAsync("at", at);
    await SecureStore.setItemAsync("rt", rt);
  };

  const logout = async () => {
    if (loggingOut.current) return;
    loggingOut.current = true;
    try {
      await unregisterFromPushNotifications().catch((error) =>
        console.warn("Push unregistration failed:", error),
      );
      await logoutTokens().catch((error) => console.warn("Token revocation failed:", error));
      setUser(null);
      await SecureStore.deleteItemAsync("at");
      await SecureStore.deleteItemAsync("rt");
      void queryClient.resetQueries();
    } finally {
      loggingOut.current = false;
    }
  };

  const loginBrowser = async (authorizationUrl: string) => {
    if (!authorizationUrl) {
      console.warn("Missing authorization URL — check EXPO_PUBLIC_* env vars");
      return;
    }

    const result = await WebBrowser.openAuthSessionAsync(authorizationUrl, REDIRECT_URI);
    if (result.type !== "success" || !result.url) return;

    const fragment = result.url.split("#")[1] ?? "";
    const params = new URLSearchParams(fragment);
    const at = params.get("at");
    const rt = params.get("rt");
    if (at && rt) {
      await saveJWT(at, rt);
    } else {
      console.warn("Failed to parse tokens from redirect URL");
    }
  };

  const loginDiscord = async () => {
    if (loggingOut.current) return;
    await logout();
    await loginBrowser(DISCORD_URL);
  };

  const userSub = user?.sub;

  useEffect(() => {
    if (!userSub) return;

    void registerForPushNotifications().catch((error) =>
      console.warn("Push registration failed:", error),
    );
    const subscription = watchForTokenRotation();
    return () => subscription.remove();
  }, [userSub]);

  useEffect(() => {
    const subscription = AppState.addEventListener("change", async (state) => {
      if (state === "active") {
        const AT = await SecureStore.getItemAsync("at");
        if (AT) {
          const expiry = jwtDecode(AT)["exp"] as number;
          const currentTime = Math.floor(Date.now() / 1000);
          if (expiry - currentTime < 60) {
            try {
              await refreshTokens();
              console.log("Tokens refreshed on app focus");
            } catch (err) {
              console.error("Failed to refresh tokens on app focus:", err);
            }
          }
        }
      }
    });

    return () => {
      subscription.remove();
    };
  }, []);

  useEffect(() => {
    const loginFromStorage = async () => {
      const at = await SecureStore.getItemAsync("at");
      const rt = await SecureStore.getItemAsync("rt");

      if (at && rt) {
        await saveJWT(at, rt);
      } else {
        console.warn("Failed to log in from storage");
      }
    };

    loginFromStorage();
  }, []);

  useEffect(() => {
    configureApi({
      baseUrl: API_URL,
      getAuthToken: async () => await SecureStore.getItemAsync("at"),
      getRefreshToken: async () => await SecureStore.getItemAsync("rt"),
      onUnauthenticated: logout,
      onTokenRefreshed: saveJWT,
    });
  }, []);

  return <AuthContext value={{ user, logout, loginBrowser, loginDiscord }}>{children}</AuthContext>;
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
