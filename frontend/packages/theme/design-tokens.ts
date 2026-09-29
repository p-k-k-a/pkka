import { PALETTE } from "./palette";

export type ThemeTokens = {
  background: string;
  foreground: string;
  card: string;
  cardForeground: string;
  popover: string;
  popoverForeground: string;
  primary: string;
  primaryForeground: string;
  secondary: string;
  secondaryForeground: string;
  muted: string;
  mutedForeground: string;
  accent: string;
  accentForeground: string;
  destructive: string;
  destructiveForeground: string;
  border: string;
  input: string;
  ring: string;
  band: string;
  brandInk: string;
  bandForeground: string;
};

// Values taken straight from PALETTE are on the Figma system; literal hex means the role
// has no counterpart there. Figma defines no dark theme and no red, so the whole dark
// column and `destructive` are code inventions — see the Figma alignment issue.
export const THEME: Record<"light" | "dark", ThemeTokens> = {
  light: {
    background: "#ffffff",
    foreground: "#16212f", // ink
    card: "#ffffff",
    cardForeground: "#16212f",
    popover: "#ffffff",
    popoverForeground: "#16212f",
    primary: PALETTE.orange[400], // brand amber
    primaryForeground: "#1a1a1a", // near-black
    secondary: PALETTE.orange["050"], // cream
    secondaryForeground: "#925f00",
    muted: "#edf1fa", // surface-blue
    mutedForeground: "#486284", // body
    accent: PALETTE.blue[400],
    accentForeground: "#ffffff",
    destructive: "#d8342c",
    destructiveForeground: "#ffffff",
    border: "#edf1fa", // line
    input: "#edf1fa",
    ring: PALETTE.blue[200],
    band: PALETTE.blue[600],
    brandInk: "#3a2300",
    bandForeground: "#edf1fa",
  },
  dark: {
    background: PALETTE.blue[700],
    foreground: "#edf1fa",
    card: "#2e4058",
    cardForeground: "#edf1fa",
    popover: "#2e4058",
    popoverForeground: "#edf1fa",
    primary: PALETTE.orange[400], // amber stays the same
    primaryForeground: "#1a1a1a",
    secondary: "#2e4058",
    secondaryForeground: "#edf1fa",
    muted: "#2e4058",
    mutedForeground: "#bacbef",
    accent: PALETTE.blue[300], // lighter blue for dark
    accentForeground: "#ffffff",
    destructive: "#d8342c",
    destructiveForeground: "#ffffff",
    border: "#47576c",
    input: "#4d5c71",
    ring: PALETTE.blue[300],
    band: PALETTE.blue[700],
    brandInk: "#3a2300",
    bandForeground: "#edf1fa",
  },
};

// Every value is opaque 6-digit hex. NativeWind rebuilds colours by argument count:
// hsl(var(--token)) is 3, with alpha 4. A translucent token under an opacity modifier
// (bg-input/50) makes 5 and the style is dropped silently, so mobile would lose it.

// 5px — this system's control radius, deliberately not shadcn's default 10px.
export const RADIUS = "0.3125rem";
