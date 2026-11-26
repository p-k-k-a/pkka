"use client";

import { useAuth } from "@/lib/auth-context";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export function PublicSessionRedirect({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      const { pathname, search, hash } = window.location;
      const hasTwin = /^\/(events|announcements)(\/|$)/.test(pathname);
      router.replace(hasTwin ? `/dashboard${pathname}${search}${hash}` : "/dashboard");
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading || isAuthenticated) {
    return null;
  }

  return children;
}
