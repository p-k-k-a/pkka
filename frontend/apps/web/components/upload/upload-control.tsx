"use client";

import { useRef, useState, type ReactNode } from "react";
import type { VariantProps } from "class-variance-authority";
import type { FileUpload } from "@pkka/api";
import { uploadErrorMessage, uploadProgressLabel, type LabelledUploadRule } from "@pkka/domain";
import { Button, type buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type UploadControlProps = {
  /** From `useFileUpload`; owned by the form so it can hold off saving while a file is in flight. */
  upload: FileUpload;
  rule: LabelledUploadRule;
  /** Button content: an icon and a label. */
  children: ReactNode;
  /** `objectKey` of the stored file, plus the picked file for a name or a local preview. */
  onUploaded: (objectKey: string, file: File) => void;
  /** Shown under the button unless there is an error to show instead. */
  hint?: ReactNode;
  variant?: VariantProps<typeof buttonVariants>["variant"];
  disabled?: boolean;
  className?: string;
};

/** A "pick a file" button that uploads the choice straight away, with progress, cancel and errors. */
export function UploadControl({
  upload,
  rule,
  children,
  onUploaded,
  hint,
  variant = "outline",
  disabled,
  className,
}: UploadControlProps) {
  const input = useRef<HTMLInputElement>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function handleChange(changeEvent: React.ChangeEvent<HTMLInputElement>) {
    const file = changeEvent.target.files?.[0];
    // Cleared so that picking the same file again (after an error or a removal) still fires.
    changeEvent.target.value = "";
    if (!file) return;

    setMessage(null);
    try {
      onUploaded(await upload.upload({ blob: file }), file);
    } catch (error) {
      setMessage(uploadErrorMessage(error, rule));
    }
  }

  const progress = uploadProgressLabel(upload.progress);

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <input
        ref={input}
        type="file"
        accept={rule.contentTypes.join(",")}
        className="sr-only"
        tabIndex={-1}
        aria-hidden="true"
        onChange={handleChange}
      />
      {upload.isUploading ? (
        <div className="flex h-[34px] items-center gap-3">
          <div
            role="progressbar"
            aria-label="Postęp przesyłania pliku"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(upload.progress * 100)}
            className="bg-muted h-2 min-w-24 flex-1 overflow-hidden rounded-full"
          >
            <div className="bg-primary h-full transition-[width]" style={{ width: progress }} />
          </div>
          <span className="text-muted-foreground w-10 text-right text-sm tabular-nums">
            {progress}
          </span>
          <Button type="button" variant="ghost" size="sm" onClick={upload.cancel}>
            Anuluj
          </Button>
        </div>
      ) : (
        <Button
          type="button"
          variant={variant}
          disabled={disabled}
          className="self-start"
          onClick={() => input.current?.click()}
        >
          {children}
        </Button>
      )}
      {message ? (
        <p role="alert" className="text-destructive text-xs font-medium">
          {message}
        </p>
      ) : (
        hint
      )}
    </div>
  );
}
