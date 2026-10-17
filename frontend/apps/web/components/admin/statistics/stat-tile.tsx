import type { ReactNode } from "react";
import { InfoTip } from "@/components/admin/statistics/info-tip";
import { Card } from "@/components/ui/card";

type StatTileProps = {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  info?: string;
  children?: ReactNode;
};

export function StatTile({ label, value, hint, info, children }: StatTileProps) {
  return (
    <Card className="gap-2 px-6 py-5">
      <div className="flex items-center gap-1.5">
        <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
          {label}
        </p>
        {info ? <InfoTip label={label}>{info}</InfoTip> : null}
      </div>
      <p className="font-heading text-foreground text-[33px] leading-none font-semibold tracking-tight">
        {value}
      </p>
      {hint ? <p className="text-muted-foreground text-sm">{hint}</p> : null}
      {children}
    </Card>
  );
}
