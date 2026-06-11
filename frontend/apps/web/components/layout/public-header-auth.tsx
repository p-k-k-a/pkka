"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { useAuth } from "@/lib/auth-context";
import { Skeleton } from "@/components/ui/skeleton";

export function PublicHeaderAuth() {
  const { isAuthenticated, isLoading, loginWithKeycloak, logout } = useAuth();

  return (
    <div className="flex items-center gap-3">
      <ThemeToggle />
      {isLoading ? (
        <Skeleton className="h-11 w-32 rounded-lg" />
      ) : isAuthenticated ? (
        <>
          <Button asChild variant="outline" size="xl">
            <Link href="/dashboard">Panel</Link>
          </Button>
          <Button variant="ghost" size="xl" onClick={logout}>
            Wyloguj
          </Button>
        </>
      ) : (
        <Button onClick={loginWithKeycloak} size="xl">
          Zaloguj się
        </Button>
      )}
    </div>
  );
}
