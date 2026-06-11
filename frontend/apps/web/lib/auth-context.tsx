"use client";

import { apiBaseUrl, backendLogoutUrl, keycloakLoginUrl } from "@/lib/api-config";
import { markAuthNavigation } from "@/lib/auth-navigation";
import type { AuthContextType } from "@/types/auth";
import { ApiError, configureApi, getMeQueryKey, me } from "@pkka/api";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useEffect, useMemo } from "react";

export const AuthContext = createContext<AuthContextType | null>(null);

configureApi({
  baseUrl: apiBaseUrl,
  sessionAuth: true,
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();

  useEffect(() => {
    configureApi({
      baseUrl: apiBaseUrl,
      sessionAuth: true,
      onUnauthenticated: async () => {
        queryClient.setQueryData(getMeQueryKey(), null);
      },
    });
  }, [queryClient]);

  const {
    data: user,
    isPending,
    refetch,
  } = useQuery({
    queryKey: getMeQueryKey(),
    queryFn: async () => {
      try {
        return (await me()).data;
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null;
        throw error;
      }
    },
    retry: false,
    refetchOnWindowFocus: true,
  });

  const isLoading = isPending;
  const isAuthenticated = user != null;

  const refreshUser = useCallback(() => refetch(), [refetch]);

  const loginWithKeycloak = useCallback(() => {
    markAuthNavigation();
    window.location.href = keycloakLoginUrl();
  }, []);

  const logout = useCallback(() => {
    window.location.href = backendLogoutUrl();
  }, []);

  const value = useMemo(
    () => ({
      user: user ?? null,
      isLoading,
      isAuthenticated,
      refreshUser,
      loginWithKeycloak,
      logout,
    }),
    [user, isLoading, isAuthenticated, refreshUser, loginWithKeycloak, logout],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
