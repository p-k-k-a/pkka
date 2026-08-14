import { BottomSheetProvider as NativeBottomSheetProvider } from "@swmansion/react-native-bottom-sheet";
import { type ReactNode } from "react";

export function BottomSheetProvider({ children }: { children: ReactNode }) {
  return <NativeBottomSheetProvider>{children}</NativeBottomSheetProvider>;
}
