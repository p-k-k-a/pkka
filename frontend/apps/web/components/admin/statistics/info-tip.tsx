import { Info } from "lucide-react";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

export function InfoTip({ label, children }: { label: string; children: string }) {
  return (
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
        {children}
      </TooltipContent>
    </Tooltip>
  );
}
