import { RADIUS, THEME, type ThemeTokens } from "./design-tokens";

const TOKEN_ORDER = Object.keys(THEME.light) as (keyof ThemeTokens)[];

function cssVarName(token: string): string {
  return token.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);
}

function round(value: number): number {
  return Number(value.toFixed(1));
}

// Tailwind v3 resolves opacity modifiers (bg-input/50) only for hsl(var(--x)),
// so mobile needs bare "H S% L%" triples. Deleting this is the whole gain of
// moving mobile to Tailwind v4.
function hexToHslTriple(hex: string): string {
  const match = /^#([0-9a-f]{6})$/i.exec(hex);
  if (!match) {
    throw new Error(`Expected an opaque 6-digit hex for the mobile palette, got "${hex}".`);
  }

  const int = Number.parseInt(match[1]!, 16);
  const r = ((int >> 16) & 255) / 255;
  const g = ((int >> 8) & 255) / 255;
  const b = (int & 255) / 255;

  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const lightness = (max + min) / 2;
  const delta = max - min;

  let hue = 0;
  let saturation = 0;
  if (delta !== 0) {
    saturation = delta / (1 - Math.abs(2 * lightness - 1));
    if (max === r) hue = ((g - b) / delta) % 6;
    else if (max === g) hue = (b - r) / delta + 2;
    else hue = (r - g) / delta + 4;
    hue *= 60;
    if (hue < 0) hue += 360;
  }

  return `${round(hue)} ${round(saturation * 100)}% ${round(lightness * 100)}%`;
}

// Web takes the hex straight; mobile needs bare "H S% L%" triples (see hexToHslTriple).
export function cssVars(scheme: "light" | "dark", format: "hex" | "hsl"): Record<string, string> {
  const palette = THEME[scheme];
  const vars: Record<string, string> = {};

  for (const token of TOKEN_ORDER) {
    const value = palette[token];
    vars[`--${cssVarName(token)}`] = format === "hex" ? value : hexToHslTriple(value);
  }

  if (scheme === "light") vars["--radius"] = RADIUS;

  return vars;
}
