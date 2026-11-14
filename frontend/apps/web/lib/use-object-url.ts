import { useEffect, useRef } from "react";

/**
 * Local preview URL for a file the user just picked. Each call replaces the previous URL,
 * and the last one is released on unmount, so previews do not pile up in memory.
 */
export function useObjectUrl() {
  const current = useRef<string | null>(null);

  const release = () => {
    if (current.current) URL.revokeObjectURL(current.current);
    current.current = null;
  };

  useEffect(() => release, []);

  return (file: Blob) => {
    release();
    current.current = URL.createObjectURL(file);
    return current.current;
  };
}
