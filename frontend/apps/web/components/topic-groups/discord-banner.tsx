"use client";

import type { ReactNode } from "react";
import { ArrowUpRight } from "lucide-react";
import { DiscordIcon } from "@pkka/icons/web";
import { DiscordStatus, useGetMyDiscordStatus } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

function Shell({ children }: { children: ReactNode }) {
  return (
    <div className="border-border flex flex-col gap-4 rounded-xl border p-5 sm:flex-row sm:items-center">
      {children}
    </div>
  );
}

export function DiscordBanner() {
  const { reloginWithDiscord } = useAuth();
  const { data } = useGetMyDiscordStatus();
  const status = data?.data.status;
  const inviteUrl = data?.data.inviteUrl;

  if (status === DiscordStatus.NOT_LINKED) {
    return (
      <Shell>
        <p className="text-foreground flex-1 font-semibold">
          Podepnij Discorda, żeby wejść na kanał grupy
        </p>
        <Button type="button" className="shrink-0" onClick={reloginWithDiscord}>
          <DiscordIcon data-icon="inline-start" className="size-4" />
          Połącz z Discordem
        </Button>
      </Shell>
    );
  }

  if (status === DiscordStatus.NOT_ON_SERVER && inviteUrl) {
    return (
      <Shell>
        <div className="flex-1 space-y-1">
          <p className="text-foreground font-semibold">Nie ma Cię na serwerze klubu</p>
          <p className="text-muted-foreground text-sm">
            Dołącz do serwera, a kanał grupy pojawi się u Ciebie na Discordzie.
          </p>
        </div>
        <Button asChild className="shrink-0">
          <a href={inviteUrl} target="_blank" rel="noreferrer">
            <DiscordIcon data-icon="inline-start" className="size-4" />
            Dołącz do serwera
            <ArrowUpRight data-icon="inline-end" />
          </a>
        </Button>
      </Shell>
    );
  }

  return null;
}
