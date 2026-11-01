export type User = {
  sub: string;
};

export type AuthContextType = {
  user: User | null;
  login: (at: string, rt: string) => Promise<void>;
  logout: () => Promise<void>;
  loginInBrowser: (authorizationUrl: string) => Promise<void>;
  verifyWithDiscord: () => Promise<void>;
};
