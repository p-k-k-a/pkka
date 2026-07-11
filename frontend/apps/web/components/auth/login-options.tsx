"use client";

import { DiscordIcon } from "@/components/auth/discord-icon";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export function LoginOptions() {
  const { loginWithKeycloak, loginWithDiscord, register } = useAuth();

  return (
    <div className="border-border/70 bg-card w-full max-w-md space-y-7 rounded-2xl border p-8 shadow-sm">
      <div className="space-y-2 text-center">
        <h1 className="text-foreground text-3xl font-extrabold tracking-tight">Zaloguj się</h1>
        <p className="text-muted-foreground text-sm leading-relaxed">
          Dołącz do społeczności Klubu Alumna Wydziału Informatyki AGH.
        </p>
      </div>

      <div className="space-y-3">
        <Button size="xl" className="w-full font-semibold" onClick={loginWithKeycloak}>
          Zaloguj się przez SSO
        </Button>

        <Button
          variant="outline"
          size="xl"
          className="w-full font-semibold"
          onClick={loginWithDiscord}
        >
          <DiscordIcon className="size-5" />
          Kontynuuj przez Discord
        </Button>
      </div>

      <div className="flex items-center gap-3">
        <div className="bg-border h-px flex-1" />
        <span className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
          Nie masz konta?
        </span>
        <div className="bg-border h-px flex-1" />
      </div>

      <Button variant="secondary" size="xl" className="w-full font-semibold" onClick={register}>
        Zarejestruj się
      </Button>
    </div>
  );
}
