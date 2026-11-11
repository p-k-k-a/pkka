import type { MeResponse } from "@pkka/api";

export type User = MeResponse;

export type AuthContextType = {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  refreshUser: () => Promise<unknown>;
  loginWithKeycloak: (returnTo?: string) => void;
  loginWithDiscord: (returnTo?: string) => void;
  register: (returnTo?: string) => void;
  logout: () => void;
  reloginWithDiscord: () => void;
};
