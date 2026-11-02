export type User = {
  sub: string;
};

export type AuthContextType = {
  user: User | null;
  logout: () => Promise<void>;
  loginBrowser: (authorizationUrl: string) => Promise<void>;
  loginDiscord: () => Promise<void>;
};
