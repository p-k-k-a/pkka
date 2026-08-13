import { useMemo, useRef, useState } from "react";
import { PanResponder, View } from "react-native";

const THUMB = 24;

type RangeSliderProps = {
  min: number;
  max: number;
  low: number;
  high: number;
  step?: number;
  onChange: (low: number, high: number) => void;
};

// Dual-thumb range slider driven by PanResponder
export function RangeSlider({ min, max, low, high, step = 1, onChange }: RangeSliderProps) {
  const [width, setWidth] = useState(0);
  const usable = Math.max(width - THUMB, 1);

  // Refs keep the responder closures reading the latest props without being
  // recreated mid-gesture.
  const lowRef = useRef(low);
  const highRef = useRef(high);
  const onChangeRef = useRef(onChange);
  lowRef.current = low;
  highRef.current = high;
  onChangeRef.current = onChange;
  const startXRef = useRef(0);

  const valueToX = (value: number) => ((value - min) / (max - min)) * usable;
  const xToValue = (x: number) => {
    const ratio = Math.min(Math.max(x / usable, 0), 1);
    return Math.round((min + ratio * (max - min)) / step) * step;
  };

  const responders = useMemo(() => {
    const make = (which: "low" | "high") =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: () => true,
        // Keep the pan once it starts — otherwise the enclosing native bottom
        // sheet's drag-to-dismiss can steal a thumb drag that has any vertical drift.
        onPanResponderTerminationRequest: () => false,
        onPanResponderGrant: () => {
          startXRef.current = valueToX(which === "low" ? lowRef.current : highRef.current);
        },
        onPanResponderMove: (_evt, gesture) => {
          const next = xToValue(startXRef.current + gesture.dx);
          if (which === "low") {
            onChangeRef.current(Math.min(next, highRef.current), highRef.current);
          } else {
            onChangeRef.current(lowRef.current, Math.max(next, lowRef.current));
          }
        },
      });
    return { low: make("low"), high: make("high") };
    // valueToX/xToValue close over `usable`, so rebuild the responders when it changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [usable]);

  return (
    <View className="py-2" onLayout={(e) => setWidth(e.nativeEvent.layout.width)}>
      <View className="h-6 justify-center">
        <View className="bg-muted h-1 rounded-full" />
        {width > 0 ? (
          <>
            <View
              className="bg-foreground absolute h-1 rounded-full"
              style={{
                left: valueToX(low) + THUMB / 2,
                width: Math.max(valueToX(high) - valueToX(low), 0),
              }}
            />
            <View
              {...responders.low.panHandlers}
              hitSlop={12}
              className="border-foreground bg-background absolute size-6 rounded-full border-2"
              // When both thumbs sit at max, lift the low thumb above the high
              // thumb so it stays grabbable (only the low thumb can still move).
              style={{ left: valueToX(low), zIndex: low === max ? 2 : 1 }}
            />
            <View
              {...responders.high.panHandlers}
              hitSlop={12}
              className="border-foreground bg-background absolute size-6 rounded-full border-2"
              style={{ left: valueToX(high), zIndex: 1 }}
            />
          </>
        ) : null}
      </View>
    </View>
  );
}
