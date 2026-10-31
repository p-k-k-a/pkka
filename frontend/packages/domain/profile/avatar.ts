import { hashString } from "../format/hash";

// Tailwind class names, shared by web and mobile (NativeWind). Both apps scan this
// package for classes: web via @source in globals.css, mobile via tailwind.config.js.
const AVATAR_COLORS = [
  { bg: "bg-emerald-100 dark:bg-emerald-950", text: "text-emerald-800 dark:text-emerald-300" },
  { bg: "bg-sky-100 dark:bg-sky-950", text: "text-sky-800 dark:text-sky-300" },
  { bg: "bg-indigo-100 dark:bg-indigo-950", text: "text-indigo-800 dark:text-indigo-300" },
  { bg: "bg-violet-100 dark:bg-violet-950", text: "text-violet-800 dark:text-violet-300" },
  { bg: "bg-amber-100 dark:bg-amber-950", text: "text-amber-800 dark:text-amber-300" },
  { bg: "bg-rose-100 dark:bg-rose-950", text: "text-rose-800 dark:text-rose-300" },
];

export function avatarInitials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const first = parts[0] ?? "";
  const last = parts.length > 1 ? (parts[parts.length - 1] ?? "") : "";
  return (last ? first.charAt(0) + last.charAt(0) : first.slice(0, 2)).toUpperCase();
}

export function avatarColors(name: string) {
  return AVATAR_COLORS[hashString(name) % AVATAR_COLORS.length] ?? AVATAR_COLORS[0]!;
}
