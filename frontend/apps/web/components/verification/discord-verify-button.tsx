"use client";

import { useGetMyProfile } from "@pkka/api";
import { DiscordIcon } from "@pkka/icons/web";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";

export function DiscordVerifyButton() {
  const { verifyWithDiscord } = useAuth();
  const { data } = useGetMyProfile();
  const profile = data?.data;

  if (!profile || profile.discordId) return null;

  return (
    <Button
      variant="outline"
      size="xl"
      className="w-full font-semibold"
      onClick={verifyWithDiscord}
    >
      <DiscordIcon className="size-5" />
      Zweryfikuj przez Discord
    </Button>
  );
}
