"use client";

import { useAuth } from "@/lib/auth-context";
import { currentPage, dashboardTwin } from "@/lib/twin-paths";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export function PublicSessionRedirect({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.replace(dashboardTwin(currentPage()));
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading || isAuthenticated) {
    return null;
  }

  return children;
}
