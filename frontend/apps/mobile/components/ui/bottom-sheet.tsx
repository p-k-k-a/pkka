import { THEME } from "@/lib/theme";
import { ModalBottomSheet } from "@swmansion/react-native-bottom-sheet";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { ScrollView, StyleSheet, View, useWindowDimensions } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type BottomSheetProps = {
  visible: boolean;
  onClose: () => void;
  children: ReactNode;
  /**
   * Open at a fixed fraction of the window height with the content scrollable,
   * instead of sizing to the content. Use when the content can grow past the
   * screen or changes height while open — the native `content` detent
   * under-measures such content and leaves the bottom of it unreachable.
   */
  heightFraction?: number;
};

// Stable references — detents/opacities are read on every render.
const DETENTS: (number | "content")[] = [0, "content"];
const SCRIM_OPACITIES = [0, 0.4];

// Native detents-based modal sheet from @swmansion/react-native-bottom-sheet.
// index 0 = closed, index 1 = open at content height. Built on react-native's
// native sheet infra (not reanimated), so it works on New Arch + reanimated 4
// where @gorhom/bottom-sheet's animation worklets silently no-op.
export function BottomSheet({ visible, onClose, children, heightFraction }: BottomSheetProps) {
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const [index, setIndex] = useState(visible ? 1 : 0);

  useEffect(() => {
    setIndex(visible ? 1 : 0);
  }, [visible]);

  // Detents are points, not fractions, so derive the fixed one from the window.
  // Memoized because the native sheet re-reads detents on every render.
  const detents = useMemo(
    () => (heightFraction ? [0, Math.round(height * heightFraction)] : DETENTS),
    [heightFraction, height],
  );

  return (
    <ModalBottomSheet
      detents={detents}
      index={index}
      onIndexChange={(next) => {
        setIndex(next);
        if (next === 0) onClose();
      }}
      scrimColor="#000000"
      scrimOpacities={SCRIM_OPACITIES}
      surface={
        <View
          style={[
            StyleSheet.absoluteFill,
            {
              backgroundColor: THEME.light.background,
              borderTopLeftRadius: 24,
              borderTopRightRadius: 24,
            },
          ]}
        />
      }
    >
      <View
        style={
          heightFraction
            ? { flex: 1, paddingTop: 12 }
            : { paddingHorizontal: 20, paddingTop: 12, paddingBottom: insets.bottom + 24 }
        }
      >
        <View
          style={{
            alignSelf: "center",
            width: 48,
            height: 5,
            borderRadius: 999,
            backgroundColor: THEME.light.mutedForeground,
            opacity: 0.3,
            marginBottom: 16,
          }}
        />
        {heightFraction ? (
          <ScrollView
            style={{ flex: 1 }}
            contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: insets.bottom + 24 }}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
          >
            {children}
          </ScrollView>
        ) : (
          children
        )}
      </View>
    </ModalBottomSheet>
  );
}
