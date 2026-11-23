"use client";

import { ExternalLink, UserCog } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { accountConsoleUrl } from "@/lib/api-config";

export function ManageAccount() {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button asChild variant="outline" className="w-full">
          <a
            href={accountConsoleUrl(`${window.location.origin}/dashboard/profile`)}
            target="_blank"
            rel="noopener noreferrer"
          >
            <UserCog data-icon="inline-start" />
            Zarządzaj kontem Keycloak
            <ExternalLink data-icon="inline-end" className="text-muted-foreground" />
          </a>
        </Button>
      </TooltipTrigger>
      <TooltipContent>Hasło, 2FA, aktywne sesje i konto Discord</TooltipContent>
    </Tooltip>
  );
}
