import { useEffect, useState } from "react";

const DEFAULT_DEBOUNCE_MS = 300;

export function useDebouncedValue<T>(value: T, delayMs = DEFAULT_DEBOUNCE_MS): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(id);
  }, [value, delayMs]);

  return debounced;
}
