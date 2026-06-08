"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { useAuth } from "@/lib/auth-context";
import { Skeleton } from "@/components/ui/skeleton";

export function PublicHeaderAuth() {
  const { user, isLoading } = useAuth();

  return (
    <div className="flex items-center gap-3">
      <ThemeToggle />
      {isLoading ? (
        <Skeleton className="h-11 w-32 rounded-lg" />
      ) : user ? null : (
        <Button asChild size="xl">
          <Link href="/login">Zaloguj się</Link>
        </Button>
      )}
    </div>
  );
}
