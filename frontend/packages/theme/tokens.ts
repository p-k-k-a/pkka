/**
 * Single source of truth for the Klub Alumnów WI AGH palette.
 *
 * Values are hex (hex8 where a token is translucent). Everything downstream is
 * generated from here by `scripts/generate.ts`:
 *   - apps/web/app/globals.css   — CSS custom properties (Tailwind v4)
 *   - apps/mobile/global.css     — bare HSL triples (Tailwind v3 + NativeWind)
 * and `THEME` below is consumed directly by React Native `color` props.
 */

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
  navy: string;
  brandInk: string;
  whiteText: string;
  chart1: string;
  chart2: string;
  chart3: string;
  chart4: string;
  chart5: string;
};

export const TOKENS: Record<"light" | "dark", ThemeTokens> = {
  light: {
    background: "#ffffff",
    foreground: "#16212f", // ink
    card: "#ffffff",
    cardForeground: "#16212f",
    popover: "#ffffff",
    popoverForeground: "#16212f",
    primary: "#f5a200", // brand amber
    primaryForeground: "#1a1a1a",
    secondary: "#fffbf2", // cream
    secondaryForeground: "#925f00",
    muted: "#edf1fa", // surface blue
    mutedForeground: "#486284", // body
    accent: "#0f6ab6", // accent blue
    accentForeground: "#ffffff",
    destructive: "#d8342c",
    destructiveForeground: "#ffffff",
    border: "#edf1fa",
    input: "#edf1fa",
    ring: "#82b3fd",
    navy: "#022749",
    brandInk: "#3a2300",
    whiteText: "#edf1fa",
    chart1: "#f5a200",
    chart2: "#0f6ab6",
    chart3: "#022749",
    chart4: "#ffd3ad",
    chart5: "#c7dafe",
  },
  dark: {
    background: "#011125",
    foreground: "#edf1fa",
    card: "#2e4058",
    cardForeground: "#edf1fa",
    popover: "#2e4058",
    popoverForeground: "#edf1fa",
    primary: "#f5a200", // amber stays the same
    primaryForeground: "#1a1a1a",
    secondary: "#2e4058",
    secondaryForeground: "#edf1fa",
    muted: "#2e4058",
    mutedForeground: "#bacbef",
    accent: "#188ef2", // lighter blue for dark
    accentForeground: "#ffffff",
    destructive: "#d8342c",
    destructiveForeground: "#ffffff",
    border: "#ffffff1f", // 12% white — hairline on dark
    input: "#ffffff26", // 15% white
    ring: "#188ef2",
    navy: "#011125",
    brandInk: "#3a2300",
    whiteText: "#edf1fa",
    chart1: "#f5a200",
    chart2: "#188ef2",
    chart3: "#bacbef",
    chart4: "#ffd3ad",
    chart5: "#c7dafe",
  },
};

/**
 * NativeWind resolves colors as `hsl(var(--token))`, which cannot carry an alpha
 * channel — so the two translucent dark tokens fall back to the solid slate the
 * mobile app already ships. (Mobile is pinned to the light scheme today; this only
 * matters once it opts into dark.)
 */
export const MOBILE_DARK_FALLBACKS: Partial<ThemeTokens> = {
  border: "#2e4058",
  input: "#2e4058",
};

/** The control radius this design system specifies. */
export const RADIUS = "0.3125rem"; // 5px

/**
 * Semantic type scale, replacing the arbitrary `text-[Npx]` values that were
 * scattered across both apps.
 *
 * Sizes are exact rem equivalents of the pixel values they replace, so rendering
 * is unchanged at the default 16px root while the web app gains root-font-size
 * scaling. Deliberately font-size only — `text-[Npx]` never set a line-height, so
 * pairing one here would shift existing layouts. Call sites keep using `leading-*`
 * where they already do.
 */
export const TYPE_SCALE = {
  eyebrow: "0.625rem", // 10px
  label: "0.6875rem", // 11px
  hint: "0.8125rem", // 13px
  body: "0.9375rem", // 15px
  "card-title": "1.125rem", // 18px
  h3: "1.4375rem", // 23px
  h2: "1.75rem", // 28px
  "h2-lg": "2.0625rem", // 33px
  display: "2.5rem", // 40px
  "display-lg": "3rem", // 48px
} as const;

/** React Native consumes hex directly; keys match the CSS token names. */
export const THEME = {
  light: { ...TOKENS.light, radius: RADIUS },
  dark: { ...TOKENS.dark, ...MOBILE_DARK_FALLBACKS, radius: RADIUS },
};
