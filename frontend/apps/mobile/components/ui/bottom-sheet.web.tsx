import { THEME } from "@/lib/theme";
import { type ReactNode } from "react";
import { Modal, Pressable, View } from "react-native";

type BottomSheetProps = {
  visible: boolean;
  onClose: () => void;
  children: ReactNode;
};

// @swmansion/react-native-bottom-sheet is a Fabric component and calls
// codegenNativeComponent, which does not exist on web — importing it breaks the
// `expo export --platform web` bundle. Web gets this Modal-based equivalent instead.
export function BottomSheet({ visible, onClose, children }: BottomSheetProps) {
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable
        accessibilityLabel="Zamknij"
        onPress={onClose}
        style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.4)", justifyContent: "flex-end" }}
      >
        <Pressable
          onPress={(e) => e.stopPropagation()}
          style={{
            backgroundColor: THEME.light.background,
            borderTopLeftRadius: 24,
            borderTopRightRadius: 24,
            paddingHorizontal: 20,
            paddingTop: 12,
            paddingBottom: 24,
          }}
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
          {children}
        </Pressable>
      </Pressable>
    </Modal>
  );
}
