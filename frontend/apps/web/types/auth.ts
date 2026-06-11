import type { MeResponse } from "@pkka/api";

export type User = MeResponse;

export type AuthContextType = {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  refreshUser: () => Promise<unknown>;
  loginWithKeycloak: () => void;
  logout: () => void;
};
