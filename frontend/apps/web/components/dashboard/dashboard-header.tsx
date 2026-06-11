"use client";

import Link from "next/link";
import { LogOut, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";

export function DashboardHeader() {
  return (
    <header className="border-border/80 bg-background/95 sticky top-0 z-50 flex h-16 shrink-0 items-center justify-between border-b px-6 backdrop-blur">
      <Link href="/dashboard" className="text-foreground text-3xl font-extrabold tracking-tighter">
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
        <Button asChild variant="ghost" size="lg">
          <Link href="/login">
            <LogOut data-icon="inline-start" />
            Wyloguj się
          </Link>
        </Button>
      </div>
    </header>
  );
}
