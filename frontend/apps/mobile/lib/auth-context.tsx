import { AuthContextType, User } from "@/types/auth";
import { configureApi, logoutTokens, refreshTokens } from "@pkka/api";
import * as SecureStore from "expo-secure-store";
import { jwtDecode } from "jwt-decode";
import { createContext, useContext, useEffect, useState } from "react";
import { AppState } from "react-native";

export const AuthContext = createContext<null | AuthContextType>(null);

type decodedJwtType = {
  sub: string;
  realm_access: {
    roles: string[];
  };
};

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);

  const login = async (at: string, rt: string) => {
    const decoded = jwtDecode(at) as decodedJwtType;
    console.warn("login run!");
    setUser({
      sub: decoded["sub"],
      role: decoded["realm_access"]["roles"].includes("alumni") ? "alumni" : "user",
    });

    await SecureStore.setItemAsync("at", at);
    await SecureStore.setItemAsync("rt", rt);
  };

  const logout = async () => {
    await logoutTokens();
    setUser(null);
    await SecureStore.deleteItemAsync("at");
    await SecureStore.deleteItemAsync("rt");
  };

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
        await login(at, rt);
      } else {
        console.warn("Failed to log in from storage");
      }
    };

    loginFromStorage();
  }, []);

  useEffect(() => {
    configureApi({
      baseUrl: process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080",
      getAuthToken: async () => await SecureStore.getItemAsync("at"),
      getRefreshToken: async () => await SecureStore.getItemAsync("rt"),
      onUnauthenticated: logout,
      onTokenRefreshed: login,
    });
  }, []);

  return <AuthContext value={{ user, login, logout }}>{children}</AuthContext>;
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
