"use client";

import { apiBaseUrl, backendLogoutUrl, keycloakLoginUrl } from "@/lib/api-config";
import type { AuthContextType, User } from "@/types/auth";
import { configureApi } from "@pkka/api";
import { createContext, useCallback, useContext, useEffect, useState } from "react";

export const AuthContext = createContext<AuthContextType | null>(null);

configureApi({
  baseUrl: apiBaseUrl,
  sessionAuth: true,
});

async function fetchCurrentUser(): Promise<User | null> {
  const res = await fetch(`${apiBaseUrl}/api/me`, { credentials: "include" });
  if (res.status === 401) return null;
  if (!res.ok) throw new Error("Failed to load session");
  return (await res.json()) as User;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    try {
      setUser(await fetchCurrentUser());
    } catch {
      setUser(null);
    }
  }, []);

  useEffect(() => {
    configureApi({
      baseUrl: apiBaseUrl,
      sessionAuth: true,
      onUnauthenticated: async () => setUser(null),
    });
  }, []);

  useEffect(() => {
    void refreshUser().finally(() => setIsLoading(false));
  }, [refreshUser]);

  const loginWithKeycloak = useCallback((idp?: "discord") => {
    window.location.href = keycloakLoginUrl(idp);
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    window.location.href = backendLogoutUrl();
  }, []);

  return (
    <AuthContext
      value={{
        user,
        isLoading,
        refreshUser,
        loginWithKeycloak,
        logout,
      }}
    >
      {children}
    </AuthContext>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
