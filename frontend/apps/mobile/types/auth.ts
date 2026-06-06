export type User = {
  sub: string;
  role: "user" | "alumni";
};

export type AuthContextType = {
  user: User | null;
  login: (at: string, rt: string) => Promise<void>;
  logout: () => void;
};
