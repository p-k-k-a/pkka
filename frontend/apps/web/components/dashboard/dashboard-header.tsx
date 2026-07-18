"use client";

import Link from "next/link";
import { LogOut, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { useAuth } from "@/lib/auth-context";

export function DashboardHeader() {
  const { logout } = useAuth();

  return (
    <header className="bg-muted z-50 flex h-16 shrink-0 items-center justify-between px-4 md:px-10">
      <Link
        href="/dashboard"
        className="font-heading text-foreground text-[23px] font-semibold tracking-tight md:text-[28px]"
      >
        PKKA
      </Link>

      <div className="flex items-center gap-2">
        <ThemeToggle />
        <Button asChild variant="outline" size="lg">
          <Link href="/dashboard/profile">
            <User data-icon="inline-start" />
            Profil
          </Link>
        </Button>
        <Button variant="ghost" size="lg" onClick={logout}>
          <LogOut data-icon="inline-start" />
          Wyloguj się
        </Button>
      </div>
    </header>
  );
}
