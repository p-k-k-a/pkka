import type { ReactNode } from "react";
import { Label } from "@/components/ui/label";

/** Uppercase accent label used by the admin editor forms. */
export function FieldLabel({ htmlFor, children }: { htmlFor?: string; children: ReactNode }) {
  return (
    <Label
      htmlFor={htmlFor}
      className="text-accent text-xs font-semibold tracking-widest uppercase"
    >
      {children}
    </Label>
  );
}
