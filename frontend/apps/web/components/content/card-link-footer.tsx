import { ArrowRight } from "lucide-react";
import { cn } from "@/lib/utils";

type CardLinkFooterProps = {
  children: React.ReactNode;
  className?: string;
};

export function CardLinkFooter({ children, className }: CardLinkFooterProps) {
  return (
    <div className={cn("mt-auto flex justify-end pt-2", className)}>
      <span className="bg-background text-accent inline-flex h-[34px] items-center gap-1.5 rounded-none px-2.5 text-sm font-medium">
        {children}
        <ArrowRight className="size-3.5" aria-hidden="true" />
      </span>
    </div>
  );
}
