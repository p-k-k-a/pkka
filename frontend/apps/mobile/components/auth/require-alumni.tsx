import { useAuth } from "@/lib/auth-context";
import { useMe } from "@pkka/api";
import { isVerifiedAlumn } from "@pkka/domain";
import { Redirect } from "expo-router";
import { type ReactNode } from "react";

export function useIsAlumni() {
  const { user } = useAuth();
  const { data } = useMe({ query: { enabled: !!user } });
  return !!user && isVerifiedAlumn(data?.data.roles);
}

export function RequireAlumni({ children }: { children: ReactNode }) {
  const isAlumni = useIsAlumni();
  if (!isAlumni) return <Redirect href="/(tabs)" />;
  return <>{children}</>;
}
