import { useAuth } from "@/lib/auth-context";
import { Redirect } from "expo-router";
import { type ReactNode } from "react";

export function useIsAlumni() {
  const { user } = useAuth();
  return user?.role === "alumni";
}

export function RequireAlumni({ children }: { children: ReactNode }) {
  const isAlumni = useIsAlumni();
  if (!isAlumni) return <Redirect href="/(tabs)" />;
  return <>{children}</>;
}
