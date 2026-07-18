import * as React from "react";

import { cn } from "@/lib/utils";

function Checkbox({ className, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type="checkbox"
      data-slot="checkbox"
      className={cn(
        "border-border accent-primary size-4 shrink-0 cursor-pointer rounded border transition-all outline-none",
        "focus-visible:ring-ring/50 focus-visible:ring-3",
        "disabled:pointer-events-none disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}

export { Checkbox };
