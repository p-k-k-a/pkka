"use client";

import { ImagePlus, Trash2 } from "lucide-react";
import type { FileUpload } from "@pkka/api";
import { AVATAR_UPLOAD, uploadLimitLabel } from "@pkka/domain";
import { UploadControl } from "@/components/upload/upload-control";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { useObjectUrl } from "@/lib/use-object-url";

/** What saving the profile should do with the picture. */
export type AvatarChange =
  | { kind: "unchanged" }
  | { kind: "replaced"; objectKey: string; previewUrl: string }
  | { kind: "removed" };

type AvatarPickerProps = {
  /** Name used for the initials shown in place of a picture. */
  fallback: string;
  /** The picture to show now: the stored one, or the preview of a new upload. */
  src?: string;
  upload: FileUpload;
  onChange: (change: AvatarChange) => void;
  disabled?: boolean;
};

/**
 * Uploads the picked picture right away, but the profile only points at it once the form
 * is saved, like every other field on the page.
 */
export function AvatarPicker({ fallback, src, upload, onChange, disabled }: AvatarPickerProps) {
  const createPreview = useObjectUrl();

  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center">
      <Avatar
        src={src}
        fallback={fallback}
        alt="Zdjęcie profilowe"
        className="size-24 text-2xl font-semibold"
      />

      <div className="flex flex-col items-center gap-2 sm:items-start">
        <div className="flex flex-wrap items-start justify-center gap-3 sm:justify-start">
          <UploadControl
            upload={upload}
            rule={AVATAR_UPLOAD}
            disabled={disabled}
            onUploaded={(objectKey, file) =>
              onChange({ kind: "replaced", objectKey, previewUrl: createPreview(file) })
            }
          >
            <ImagePlus data-icon="inline-start" aria-hidden="true" />
            {src ? "Zmień zdjęcie" : "Dodaj zdjęcie"}
          </UploadControl>
          {src && !upload.isUploading ? (
            <Button
              type="button"
              variant="ghost"
              disabled={disabled}
              onClick={() => onChange({ kind: "removed" })}
            >
              <Trash2 data-icon="inline-start" aria-hidden="true" />
              Usuń zdjęcie
            </Button>
          ) : null}
        </div>
        <p className="text-muted-foreground text-center text-[13px] sm:text-left">
          {AVATAR_UPLOAD.formats}, do {uploadLimitLabel(AVATAR_UPLOAD)}. Bez zdjęcia pokazujemy
          Twoje inicjały.
        </p>
      </div>
    </div>
  );
}
