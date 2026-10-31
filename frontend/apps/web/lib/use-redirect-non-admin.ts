"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { isAdmin } from "@pkka/domain";
import { useAuth } from "@/lib/auth-context";

/** Sends a signed-in non-admin back to the dashboard, as the other admin pages do. */
export function useRedirectNonAdmin(enabled: boolean) {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const shouldRedirect = enabled && !isLoading && !isAdmin(user?.roles);

  useEffect(() => {
    if (shouldRedirect) router.replace("/dashboard");
  }, [shouldRedirect, router]);
}
