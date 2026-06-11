import { cn } from "@/lib/utils";

type CardLinkFooterProps = {
  children: React.ReactNode;
  align?: "start" | "end";
  className?: string;
};

export function CardLinkFooter({ children, align = "end", className }: CardLinkFooterProps) {
  return (
    <div
      className={cn(
        "border-border/70 mt-6 flex items-center border-t pt-6",
        align === "start" ? "justify-start" : "justify-end",
        className,
      )}
    >
      <span className="text-foreground flex items-center gap-1.5 text-xs font-bold tracking-widest uppercase transition-transform group-hover:translate-x-1">
        {children}
      </span>
    </div>
  );
}
