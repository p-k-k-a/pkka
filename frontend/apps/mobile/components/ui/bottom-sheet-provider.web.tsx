import { type ReactNode } from "react";

// The native sheet's portal host has no web counterpart — bottom-sheet.web.tsx
// renders through a Modal instead, so the provider is a passthrough here.
export function BottomSheetProvider({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
