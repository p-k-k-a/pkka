"use client";

import { ImageIcon, ImagePlus, Trash2 } from "lucide-react";
import type { FileUpload } from "@pkka/api";
import { uploadLimitLabel, type LabelledUploadRule } from "@pkka/domain";
import { UploadControl } from "@/components/upload/upload-control";
import { Button } from "@/components/ui/button";
import { useObjectUrl } from "@/lib/use-object-url";

export type CoverValue = {
  /** What goes into the request: the `objectKey` of a new upload, or the URL the API returned. */
  reference?: string;
  /** What the browser can show: a local preview of a new upload, or the URL the API returned. */
  previewUrl?: string;
};

type CoverUploadFieldProps = {
  value: CoverValue;
  onChange: (value: CoverValue) => void;
  upload: FileUpload;
  rule: LabelledUploadRule;
};

/** Cover image picker for the admin editor forms: preview, upload, replace, remove. */
export function CoverUploadField({ value, onChange, upload, rule }: CoverUploadFieldProps) {
  const createPreview = useObjectUrl();
  // A cover saved before uploads existed can be anything; only show what a browser can load.
  const previewUrl = /^(https?:|blob:)/i.test(value.previewUrl ?? "") ? value.previewUrl : null;

  return (
    <div className="space-y-4">
      <div className="bg-background border-border text-muted-foreground relative flex aspect-[16/9] w-full max-w-xl items-center justify-center overflow-hidden rounded-lg border-2 border-dashed">
        {previewUrl ? (
          // A plain <img>: next/image cannot show a local blob: preview.
          // eslint-disable-next-line @next/next/no-img-element
          <img src={previewUrl} alt="Podgląd okładki" className="size-full object-cover" />
        ) : (
          <ImageIcon className="size-8" aria-hidden="true" />
        )}
      </div>
      <div className="flex flex-wrap items-start gap-3">
        <UploadControl
          upload={upload}
          rule={rule}
          onUploaded={(objectKey, file) =>
            onChange({ reference: objectKey, previewUrl: createPreview(file) })
          }
          hint={
            <p className="text-muted-foreground text-xs">
              {rule.formats}, do {uploadLimitLabel(rule)}. Najlepiej w proporcjach 16:9.
            </p>
          }
        >
          <ImagePlus data-icon="inline-start" aria-hidden="true" />
          {value.reference ? "Zmień okładkę" : "Wgraj okładkę"}
        </UploadControl>
        {value.reference && !upload.isUploading ? (
          <Button type="button" variant="ghost" onClick={() => onChange({})}>
            <Trash2 data-icon="inline-start" aria-hidden="true" />
            Usuń okładkę
          </Button>
        ) : null}
      </div>
    </div>
  );
}
