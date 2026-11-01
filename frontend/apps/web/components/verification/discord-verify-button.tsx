"use client";

import { DiscordIcon } from "@pkka/icons/web";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export function DiscordVerifyButton() {
  const { reloginWithDiscord } = useAuth();

  return (
    <Button
      variant="outline"
      size="xl"
      className="w-full font-semibold"
      onClick={reloginWithDiscord}
    >
      <DiscordIcon className="size-5" />
      Zweryfikuj przez Discord
    </Button>
  );
}
