import type { ReactNode } from "react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type StatTileProps = {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  children?: ReactNode;
  className?: string;
};

export function StatTile({ label, value, hint, children, className }: StatTileProps) {
  return (
    <Card className={cn("gap-2 px-6 py-5", className)}>
      <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
        {label}
      </p>
      <p className="font-heading text-foreground text-[33px] leading-none font-semibold tracking-tight">
        {value}
      </p>
      {hint ? <p className="text-muted-foreground text-sm">{hint}</p> : null}
      {children}
    </Card>
  );
}
