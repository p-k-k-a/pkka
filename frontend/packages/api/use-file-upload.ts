"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import {
  uploadFile,
  type PresignUpload,
  type UploadError,
  type UploadInput,
  type UploadRule,
} from "./upload";

/**
 * `uploadFile` with loading, progress and error state. One upload at a time: starting another
 * cancels the one in flight, and so does unmounting.
 */
export function useFileUpload({ rule, presign }: { rule: UploadRule; presign: PresignUpload }) {
  const [progress, setProgress] = useState(0);
  const controller = useRef<AbortController | null>(null);

  useEffect(() => () => controller.current?.abort(), []);

  const mutation = useMutation<string, UploadError, UploadInput>({
    mutationFn: (input) => {
      controller.current?.abort();
      const current = new AbortController();
      controller.current = current;
      setProgress(0);
      return uploadFile({
        ...input,
        rule,
        presign,
        onProgress: setProgress,
        signal: current.signal,
      });
    },
  });

  return {
    /** Resolves with the `objectKey`; rejects with an `UploadError`. */
    upload: mutation.mutateAsync,
    cancel: () => controller.current?.abort(),
    reset: mutation.reset,
    isUploading: mutation.isPending,
    /** Uploaded fraction of the current file, 0 to 1. */
    progress,
    error: mutation.error,
  };
}

export type FileUpload = ReturnType<typeof useFileUpload>;
