/**
 * Regenerates the token blocks in both apps from `tokens.ts`.
 *
 *   pnpm --filter @pkka/theme tokens
 *
 * Only the regions between the BEGIN/END markers are rewritten; everything else
 * in those files (the `@theme inline` map, `.rich-text`, fonts, the mobile colour
 * mapping) is left alone. Run this after editing `tokens.ts` and commit the result.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { MOBILE_DARK_FALLBACKS, RADIUS, TOKENS, TYPE_SCALE, type ThemeTokens } from "../tokens.ts";

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, "../../..");
const WEB_CSS = resolve(ROOT, "apps/web/app/globals.css");
const MOBILE_CSS = resolve(ROOT, "apps/mobile/global.css");
const MOBILE_TW = resolve(ROOT, "apps/mobile/tailwind.config.js");

const begin = (id: string) =>
  `/* @pkka/theme:begin:${id} — generated from packages/theme/tokens.ts, do not edit */`;
const end = (id: string) => `/* @pkka/theme:end:${id} */`;

/** `cardForeground` -> `card-foreground`, `chart1` -> `chart-1`. */
function cssVarName(key: string): string {
  return key.replace(/([a-z])([A-Z0-9])/g, "$1-$2").toLowerCase();
}

function hexToHslTriple(hex: string): string {
  const value = hex.replace("#", "");
  if (value.length !== 6) {
    throw new Error(`Expected an opaque 6-digit hex for the mobile palette, got "${hex}"`);
  }
  const r = parseInt(value.slice(0, 2), 16) / 255;
  const g = parseInt(value.slice(2, 4), 16) / 255;
  const b = parseInt(value.slice(4, 6), 16) / 255;

  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const delta = max - min;
  const l = (max + min) / 2;

  let h = 0;
  if (delta !== 0) {
    if (max === r) h = ((g - b) / delta) % 6;
    else if (max === g) h = (b - r) / delta + 2;
    else h = (r - g) / delta + 4;
    h *= 60;
    if (h < 0) h += 360;
  }

  const s = delta === 0 ? 0 : delta / (1 - Math.abs(2 * l - 1));
  const round = (n: number) => Math.round(n * 10) / 10;
  return `${round(h)} ${round(s * 100)}% ${round(l * 100)}%`;
}

function replaceRegion(
  file: string,
  source: string,
  id: string,
  body: string,
  endIndent = "",
): string {
  const startTag = begin(id);
  const endTag = end(id);
  const start = source.indexOf(startTag);
  const stop = source.indexOf(endTag);
  if (start === -1 || stop === -1) {
    throw new Error(
      `Missing "@pkka/theme:${id}" markers in ${relative(ROOT, file)}. ` +
        `Re-add the BEGIN/END comment pair around the generated block.`,
    );
  }
  return (
    source.slice(0, start) +
    startTag +
    "\n" +
    body +
    endIndent +
    endTag +
    source.slice(stop + endTag.length)
  );
}

const colorVars = (tokens: ThemeTokens, indent: string, format: (hex: string) => string) =>
  Object.entries(tokens)
    .map(([key, value]) => `${indent}--${cssVarName(key)}: ${format(value)};`)
    .join("\n");

const identity = (hex: string) => hex;

function webColors(): string {
  return [
    ":root {",
    colorVars(TOKENS.light, "  ", identity),
    `  --radius: ${RADIUS};`,
    "}",
    "",
    ".dark {",
    colorVars(TOKENS.dark, "  ", identity),
    "}",
    "",
  ].join("\n");
}

/** Lives inside `@theme inline`, where Tailwind v4 turns `--text-*` into utilities. */
function webTypeScale(): string {
  return (
    Object.entries(TYPE_SCALE)
      .map(([name, size]) => `  --text-${name}: ${size};`)
      .join("\n") + "\n"
  );
}

function mobileColors(): string {
  return [
    "@layer base {",
    "  :root {",
    colorVars(TOKENS.light, "    ", hexToHslTriple),
    `    --radius: ${RADIUS};`,
    "  }",
    "",
    "  .dark:root {",
    colorVars({ ...TOKENS.dark, ...MOBILE_DARK_FALLBACKS }, "    ", hexToHslTriple),
    "  }",
    "}",
    "",
  ].join("\n");
}

/** Lives inside `theme.extend.fontSize` in the mobile Tailwind config. */
function mobileTypeScale(): string {
  return (
    Object.entries(TYPE_SCALE)
      .map(([name, size]) => {
        const key = /^[A-Za-z_$][\w$]*$/.test(name) ? name : `"${name}"`;
        return `        ${key}: "${size}",`;
      })
      .join("\n") + "\n"
  );
}

const jsBegin = (id: string) =>
  `// @pkka/theme:begin:${id} — generated from packages/theme/tokens.ts, do not edit`;
const jsEnd = (id: string) => `// @pkka/theme:end:${id}`;

function replaceJsRegion(file: string, source: string, id: string, body: string): string {
  const startTag = jsBegin(id);
  const endTag = jsEnd(id);
  const start = source.indexOf(startTag);
  const stop = source.indexOf(endTag);
  if (start === -1 || stop === -1) {
    throw new Error(`Missing "@pkka/theme:${id}" markers in ${relative(ROOT, file)}.`);
  }
  return (
    source.slice(0, start) +
    startTag +
    "\n" +
    body +
    "        " +
    endTag +
    source.slice(stop + endTag.length)
  );
}

let web = readFileSync(WEB_CSS, "utf8");
web = replaceRegion(WEB_CSS, web, "colors", webColors());
web = replaceRegion(WEB_CSS, web, "type", webTypeScale(), "  ");
writeFileSync(WEB_CSS, web);

let mobile = readFileSync(MOBILE_CSS, "utf8");
mobile = replaceRegion(MOBILE_CSS, mobile, "colors", mobileColors());
writeFileSync(MOBILE_CSS, mobile);

let mobileTw = readFileSync(MOBILE_TW, "utf8");
mobileTw = replaceJsRegion(MOBILE_TW, mobileTw, "type", mobileTypeScale());
writeFileSync(MOBILE_TW, mobileTw);

for (const file of [WEB_CSS, MOBILE_CSS, MOBILE_TW]) {
  console.log(`@pkka/theme: wrote ${relative(ROOT, file)}`);
}
