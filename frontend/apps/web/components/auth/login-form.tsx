"use client";

import { DiscordIcon } from "@/components/auth/discord-icon";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/lib/auth-context";
import Link from "next/link";

export function LoginForm() {
  const { loginWithKeycloak } = useAuth();

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-lg items-center px-6 py-12">
      <Card className="border-border/70 w-full rounded-[24px] shadow-sm">
        <CardHeader className="space-y-3">
          <CardTitle className="text-3xl font-extrabold tracking-tight">Zaloguj się</CardTitle>
          <p className="text-muted-foreground leading-relaxed">
            Zaloguj się przez Keycloak, aby dołączyć do społeczności klubu alumnów wydziału
            informatyki AGH.
          </p>
        </CardHeader>

        <CardContent className="space-y-5">
          <Button size="lg" className="w-full rounded-xl font-semibold" onClick={() => loginWithKeycloak()}>
            Zaloguj się przez SSO
          </Button>

          <div className="flex items-center gap-3">
            <div className="bg-border h-px flex-1" />
            <span className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
              lub
            </span>
            <div className="bg-border h-px flex-1" />
          </div>

          <Button
            variant="outline"
            size="lg"
            className="w-full rounded-xl font-semibold"
            onClick={() => loginWithKeycloak("discord")}
          >
            <DiscordIcon className="size-5" />
            Kontynuuj przez Discord
          </Button>

          <p className="text-muted-foreground text-center text-sm">
            Nie masz konta?{" "}
            <button
              type="button"
              className="text-foreground font-semibold underline-offset-4 hover:underline"
              onClick={() => loginWithKeycloak()}
            >
              Zarejestruj się
            </button>
          </p>

          <p className="text-muted-foreground text-center text-sm">
            <Link href="/" className="hover:text-foreground underline-offset-4 hover:underline">
              Wróć na stronę główną
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
