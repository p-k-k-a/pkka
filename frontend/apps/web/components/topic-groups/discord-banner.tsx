"use client";

import { ArrowUpRight } from "lucide-react";
import { DiscordIcon } from "@pkka/icons/web";
import { DiscordStatus, useGetMyDiscordStatus } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export function DiscordBanner() {
  const { reloginWithDiscord } = useAuth();
  const { data } = useGetMyDiscordStatus();
  const status = data?.data.status;
  const inviteUrl = data?.data.inviteUrl;

  if (status === DiscordStatus.NOT_LINKED) {
    return (
      <div className="bg-muted flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-foreground font-semibold">
          Podepnij Discorda, żeby wejść na kanał grupy
        </p>
        <Button type="button" variant="outline" className="shrink-0" onClick={reloginWithDiscord}>
          <DiscordIcon data-icon="inline-start" className="size-4" />
          Połącz z Discordem
        </Button>
      </div>
    );
  }

  if (status === DiscordStatus.NOT_ON_SERVER && inviteUrl) {
    return (
      <div className="bg-muted flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <p className="text-foreground font-semibold">Nie ma Cię na serwerze klubu</p>
          <p className="text-muted-foreground text-sm">
            Dołącz do serwera, a kanał grupy pojawi się u Ciebie na Discordzie.
          </p>
        </div>
        <Button asChild variant="outline" className="shrink-0">
          <a href={inviteUrl} target="_blank" rel="noreferrer">
            <DiscordIcon data-icon="inline-start" className="size-4" />
            Dołącz do serwera
            <ArrowUpRight data-icon="inline-end" />
          </a>
        </Button>
      </div>
    );
  }

  return null;
}
