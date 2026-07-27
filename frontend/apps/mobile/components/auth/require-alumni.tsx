import { useAuth } from "@/lib/auth-context";
import { Redirect } from "expo-router";
import * as React from "react";

// Single source of truth for the verified-alumni gate. `user` is null while the
// session loads, so this fails closed — "loading" is treated as "not an alumni".
export function useIsAlumni() {
  const { user } = useAuth();
  return user?.role === "alumni";
}

// Route guard for directory routes: redirects non-alumni away so a deep link
// can't reach alumni-only content.
export function RequireAlumni({ children }: { children: React.ReactNode }) {
  const isAlumni = useIsAlumni();
  if (!isAlumni) return <Redirect href="/(tabs)" />;
  return <>{children}</>;
}
