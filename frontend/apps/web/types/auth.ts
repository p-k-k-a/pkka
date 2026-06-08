export type User = {
  username: string;
  name: string | null;
  email: string | null;
  roles: string[];
};

export type AuthContextType = {
  user: User | null;
  isLoading: boolean;
  refreshUser: () => Promise<void>;
  loginWithKeycloak: (idp?: "discord") => void;
  logout: () => void;
};
