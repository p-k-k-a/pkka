"use client";

import { useSyncExternalStore } from "react";

/**
 * Temporary stand-in for a profile picture API the backend does not expose yet:
 * the image is kept in localStorage as a data URL. Once `ProfileResponse` gains
 * an avatar URL and an upload endpoint exists, only this module changes — the
 * screens read the picture through `useProfileAvatar` / `readProfileAvatar` and
 * write it through `saveProfileAvatar`.
 */

const AVATAR_STORAGE_KEY = "profile-avatar";
const AVATAR_CHANGE_EVENT = "profileavatarchange";

/** Edge of the stored square, in px — covers the 112px hero avatar on retina screens. */
const AVATAR_EDGE_PX = 512;
const AVATAR_QUALITY = 0.85;

export const ACCEPTED_AVATAR_TYPES = ["image/jpeg", "image/png", "image/webp"] as const;
export const MAX_AVATAR_FILE_MB = 5;

const MAX_AVATAR_FILE_BYTES = MAX_AVATAR_FILE_MB * 1024 * 1024;

/** Message is user-facing and shown as-is next to the picker. */
export class AvatarError extends Error {}

function readStoredAvatar(): string | null {
  return window.localStorage.getItem(AVATAR_STORAGE_KEY);
}

// The server has no access to the stored picture, so it renders the initials
// fallback; the client swaps in the image right after hydration.
function getServerSnapshot(): string | null {
  return null;
}

function subscribe(callback: () => void) {
  window.addEventListener("storage", callback);
  window.addEventListener(AVATAR_CHANGE_EVENT, callback);

  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener(AVATAR_CHANGE_EVENT, callback);
  };
}

export function useProfileAvatar(): string | null {
  return useSyncExternalStore(subscribe, readStoredAvatar, getServerSnapshot);
}

/** One-shot read for components that only need the initial value, e.g. form state. */
export function readProfileAvatar(): string | null {
  return typeof window === "undefined" ? null : readStoredAvatar();
}

export function saveProfileAvatar(dataUrl: string | null) {
  try {
    if (dataUrl) window.localStorage.setItem(AVATAR_STORAGE_KEY, dataUrl);
    else window.localStorage.removeItem(AVATAR_STORAGE_KEY);
  } catch {
    throw new AvatarError("Zabrakło miejsca na zapisanie zdjęcia. Wybierz mniejszy plik.");
  }

  window.dispatchEvent(new Event(AVATAR_CHANGE_EVENT));
}

/**
 * Validates the picked file and normalises it to a square data URL, so the
 * stored payload stays small no matter what the user selected.
 */
export async function readAvatarFile(file: File): Promise<string> {
  if (!ACCEPTED_AVATAR_TYPES.includes(file.type as (typeof ACCEPTED_AVATAR_TYPES)[number])) {
    throw new AvatarError("Wybierz plik w formacie JPG, PNG lub WebP.");
  }
  if (file.size > MAX_AVATAR_FILE_BYTES) {
    throw new AvatarError(`Zdjęcie może mieć maksymalnie ${MAX_AVATAR_FILE_MB} MB.`);
  }

  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file);
  } catch {
    throw new AvatarError("Nie udało się odczytać tego pliku. Spróbuj z innym zdjęciem.");
  }

  try {
    const canvas = document.createElement("canvas");
    canvas.width = AVATAR_EDGE_PX;
    canvas.height = AVATAR_EDGE_PX;

    const context = canvas.getContext("2d");
    if (!context) throw new AvatarError("Przeglądarka nie pozwoliła przetworzyć zdjęcia.");

    // JPEG has no alpha channel, so transparent areas would turn black.
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, AVATAR_EDGE_PX, AVATAR_EDGE_PX);

    const edge = Math.min(bitmap.width, bitmap.height);
    context.drawImage(
      bitmap,
      (bitmap.width - edge) / 2,
      (bitmap.height - edge) / 2,
      edge,
      edge,
      0,
      0,
      AVATAR_EDGE_PX,
      AVATAR_EDGE_PX,
    );

    return canvas.toDataURL("image/jpeg", AVATAR_QUALITY);
  } finally {
    bitmap.close();
  }
}
