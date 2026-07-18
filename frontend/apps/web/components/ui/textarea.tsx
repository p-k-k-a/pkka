import * as React from "react";

import { cn } from "@/lib/utils";

function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "border-border bg-background text-foreground placeholder:text-muted-foreground flex min-h-20 w-full rounded-lg border px-3 py-2 text-sm transition-all outline-none",
        "focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-3",
        "disabled:pointer-events-none disabled:opacity-50",
        "aria-invalid:border-destructive aria-invalid:ring-destructive/20 aria-invalid:ring-3",
        className,
      )}
      {...props}
    />
  );
}

export { Textarea };
