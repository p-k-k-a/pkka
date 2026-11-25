import { useAuth } from "@/lib/auth-context";
import { useMe } from "@pkka/api";
import { isAdmin, isVerifiedAlumn } from "@pkka/domain";
import { Redirect } from "expo-router";
import { type ReactNode } from "react";

export function useIsAlumni() {
  const { user } = useAuth();
  const { data } = useMe({ query: { enabled: !!user } });
  return !!user && isVerifiedAlumn(data?.data.roles);
}

export function useSeesAllMaterials() {
  const { user } = useAuth();
  const { data } = useMe({ query: { enabled: !!user } });
  const roles = data?.data.roles;
  return { isLoading: !!user && !data, seesAll: isVerifiedAlumn(roles) || isAdmin(roles) };
}

export function RequireSignedIn({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Redirect href="/login" />;
  return <>{children}</>;
}

export function RequireAlumni({ children }: { children: ReactNode }) {
  const isAlumni = useIsAlumni();
  if (!isAlumni) return <Redirect href="/(tabs)" />;
  return <>{children}</>;
}
