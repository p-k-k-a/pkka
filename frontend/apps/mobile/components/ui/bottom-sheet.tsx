import { useThemeColors } from "@/lib/theme";
import { ModalBottomSheet } from "@swmansion/react-native-bottom-sheet";
import { useEffect, useState, type ReactNode } from "react";
import { ScrollView, StyleSheet, useWindowDimensions, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type BottomSheetProps = {
  visible: boolean;
  onClose: () => void;
  children: ReactNode;
};

const DETENTS: (number | "content")[] = [0, "content"];
const SCRIM_OPACITIES = [0, 0.4];

export function BottomSheet({ visible, onClose, children }: BottomSheetProps) {
  const theme = useThemeColors();
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const [index, setIndex] = useState(visible ? 1 : 0);

  useEffect(() => {
    setIndex(visible ? 1 : 0);
  }, [visible]);

  return (
    <ModalBottomSheet
      detents={DETENTS}
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
              backgroundColor: theme.background,
              borderTopLeftRadius: 24,
              borderTopRightRadius: 24,
            },
          ]}
        />
      }
    >
      <View style={{ paddingTop: 12 }}>
        <View
          style={{
            alignSelf: "center",
            width: 48,
            height: 5,
            borderRadius: 999,
            backgroundColor: theme.mutedForeground,
            opacity: 0.3,
            marginBottom: 16,
          }}
        />
        <ScrollView
          style={{ maxHeight: windowHeight - insets.top - 64 }}
          contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: insets.bottom + 24 }}
          keyboardShouldPersistTaps="handled"
          bounces={false}
        >
          {children}
        </ScrollView>
      </View>
    </ModalBottomSheet>
  );
}
