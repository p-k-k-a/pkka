"use client";

import { useRef, useState } from "react";
import { ImagePlus, Trash2 } from "lucide-react";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  ACCEPTED_AVATAR_TYPES,
  AvatarError,
  MAX_AVATAR_FILE_MB,
  readAvatarFile,
} from "@/lib/profile-avatar";

type AvatarPickerProps = {
  value: string | null;
  /** Name used for the initials shown until a picture is chosen. */
  fallback: string;
  onChange: (dataUrl: string | null) => void;
  disabled?: boolean;
};

export function AvatarPicker({ value, fallback, onChange, disabled }: AvatarPickerProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [isReading, setIsReading] = useState(false);

  const isBusy = disabled || isReading;

  async function handleFile(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Clearing the value lets the same file be picked again after a removal.
    event.target.value = "";
    if (!file) return;

    setError(null);
    setIsReading(true);
    try {
      onChange(await readAvatarFile(file));
    } catch (readError) {
      setError(
        readError instanceof AvatarError
          ? readError.message
          : "Nie udało się przetworzyć zdjęcia. Spróbuj ponownie.",
      );
    } finally {
      setIsReading(false);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center">
        <Avatar
          src={value ?? undefined}
          fallback={fallback}
          alt="Podgląd zdjęcia profilowego"
          className="size-24 text-2xl font-semibold"
        />

        <div className="flex flex-col items-center gap-2 sm:items-start">
          <div className="flex flex-wrap justify-center gap-2 sm:justify-start">
            <Button
              type="button"
              variant="outline"
              disabled={isBusy}
              onClick={() => inputRef.current?.click()}
            >
              <ImagePlus data-icon="inline-start" aria-hidden="true" />
              {isReading ? "Wczytywanie…" : value ? "Zmień zdjęcie" : "Wybierz zdjęcie"}
            </Button>
            {value ? (
              <Button
                type="button"
                variant="ghost"
                disabled={isBusy}
                onClick={() => {
                  setError(null);
                  onChange(null);
                }}
              >
                <Trash2 data-icon="inline-start" aria-hidden="true" />
                Usuń
              </Button>
            ) : null}
          </div>

          <p className="text-muted-foreground text-center text-[13px] sm:text-left">
            JPG, PNG lub WebP, maksymalnie {MAX_AVATAR_FILE_MB} MB. Zdjęcie zostanie przycięte do
            kwadratu.
          </p>
        </div>
      </div>

      {error ? <p className="text-destructive text-[13px] font-medium">{error}</p> : null}

      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_AVATAR_TYPES.join(",")}
        className="hidden"
        onChange={(event) => void handleFile(event)}
      />
    </div>
  );
}
