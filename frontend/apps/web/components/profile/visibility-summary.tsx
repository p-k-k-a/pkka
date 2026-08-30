import { Eye, EyeOff } from "lucide-react";
import type { ProfileVisibility } from "@pkka/api";
import { cn } from "@/lib/utils";

type VisibilitySummaryProps = {
  visibility: ProfileVisibility;
  discordConnected: boolean;
};

export function VisibilitySummary({ visibility, discordConnected }: VisibilitySummaryProps) {
  const rows = [
    { label: "Imię i nazwisko", visible: visibility.name, available: true },
    { label: "E-mail", visible: visibility.email, available: true },
    { label: "Discord", visible: visibility.discord, available: discordConnected },
  ];

  return (
    <ul className="divide-border divide-y">
      {rows.map((row) => (
        <li
          key={row.label}
          className="flex items-center justify-between gap-4 py-2 first:pt-0 last:pb-0"
        >
          <span className="text-muted-foreground text-[13px]">{row.label}</span>
          {row.available ? (
            <span
              className={cn(
                "inline-flex items-center gap-1.5 text-[11px] font-bold tracking-widest uppercase",
                row.visible ? "text-accent" : "text-muted-foreground",
              )}
            >
              {row.visible ? (
                <Eye className="size-3.5" aria-hidden="true" />
              ) : (
                <EyeOff className="size-3.5" aria-hidden="true" />
              )}
              {row.visible ? "Widoczne" : "Ukryte"}
            </span>
          ) : (
            <span className="text-muted-foreground text-[11px] font-semibold tracking-widest uppercase">
              Brak konta
            </span>
          )}
        </li>
      ))}
    </ul>
  );
}
