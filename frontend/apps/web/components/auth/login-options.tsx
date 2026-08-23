"use client";

import { EYEBROW } from "@pkka/theme";
import { ArrowRight } from "lucide-react";
import { DiscordIcon } from "@pkka/icons";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export function LoginOptions() {
  const { loginWithKeycloak, loginWithDiscord, register } = useAuth();

  return (
    <div className="bg-background w-full max-w-md space-y-7 rounded-lg p-8 md:p-10">
      <div className="space-y-2 text-center">
        <h1 className="font-heading text-foreground text-h2 md:text-h2-lg font-semibold tracking-tight">
          Zaloguj się
        </h1>
        <p className="text-muted-foreground text-sm leading-relaxed md:text-base">
          Dołącz do społeczności Klubu Alumnów Wydziału Informatyki AGH.
        </p>
      </div>

      <div className="space-y-3">
        <Button size="xl" className="w-full gap-2 font-semibold" onClick={loginWithKeycloak}>
          Zaloguj się przez SSO
          <ArrowRight data-icon="inline-end" />
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
        <span className={EYEBROW}>Nie masz konta?</span>
        <div className="bg-border h-px flex-1" />
      </div>

      <Button
        variant="secondary"
        size="xl"
        className="w-full gap-2 font-semibold"
        onClick={register}
      >
        Zarejestruj się
        <ArrowRight data-icon="inline-end" />
      </Button>
    </div>
  );
}
