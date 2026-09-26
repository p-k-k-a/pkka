// The Klub Alumnów colour ramps, transcribed from the "Kolory" section of the Figma
// design system. Names and steps mirror the Figma variables exactly, so a value can be
// traced back by searching the ramp name there.
//
// This is the primitive layer: raw colours, named after how they look. Components must
// not reach for it — they use the semantic tokens in design-tokens.ts, which is what
// carries light/dark switching. Reach here only when defining a new semantic token.
export const PALETTE = {
  orange: {
    "050": "#fffbf2",
    100: "#fff4db",
    200: "#ffe4a3",
    300: "#ffd06b",
    400: "#f5a200",
    500: "#d88e00",
    600: "#b87700",
    700: "#8f5d00",
    800: "#6e4700",
    900: "#533500",
  },
  blue: {
    "050": "#f3f8ff",
    "075": "#e7f1ff",
    100: "#c7dafe",
    200: "#82b3fd",
    300: "#188ef2",
    400: "#0f6ab6",
    500: "#07477d",
    600: "#022749",
    700: "#011125",
  },
  neutralBlue: {
    100: "#f5f7fa",
    300: "#d9e0ea",
    500: "#7c8798",
  },
  neutral: {
    100: "#f7f4ee",
    200: "#e9e4d8",
    300: "#ddd7cc",
    500: "#a79f92",
    700: "#756f64",
    900: "#121110",
  },
  grey: {
    "00": "#fcfcfc",
    100: "#dcdad9",
    200: "#b8b2af",
    300: "#918c8a",
    400: "#6c6866",
    500: "#494645",
    600: "#292726",
    700: "#121110",
  },
} as const;
