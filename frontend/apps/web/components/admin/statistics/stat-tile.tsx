import type { ReactNode } from "react";
import { Info } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

type StatTileProps = {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  info?: string;
  children?: ReactNode;
  className?: string;
};

export function StatTile({ label, value, hint, info, children, className }: StatTileProps) {
  return (
    <Card className={cn("gap-2 px-6 py-5", className)}>
      <div className="flex items-center gap-1.5">
        <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
          {label}
        </p>
        {info ? (
          <Tooltip>
            <TooltipTrigger asChild>
              <button
                type="button"
                aria-label={`Co oznacza „${label}”?`}
                className="text-muted-foreground hover:text-foreground focus-visible:ring-ring/50 relative -m-1 rounded-full p-1 transition-colors outline-none before:absolute before:-inset-2 focus-visible:ring-2"
              >
                <Info className="size-3.5" aria-hidden />
              </button>
            </TooltipTrigger>
            <TooltipContent side="top" className="text-pretty">
              {info}
            </TooltipContent>
          </Tooltip>
        ) : null}
      </div>
      <p className="font-heading text-foreground text-[33px] leading-none font-semibold tracking-tight">
        {value}
      </p>
      {hint ? <p className="text-muted-foreground text-sm">{hint}</p> : null}
      {children}
    </Card>
  );
}
