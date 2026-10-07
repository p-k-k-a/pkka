"use client";

import { useEffect, useRef, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useDebouncedValue } from "@/lib/use-debounced-value";

const SEARCH_DEBOUNCE_MS = 300;

type MaterialSearchInputProps = {
  id: string;
  /** The query currently applied to the list. */
  value: string;
  /** Called with the trimmed text once typing pauses. */
  onCommit: (query: string) => void;
};

export function MaterialSearchInput({ id, value, onCommit }: MaterialSearchInputProps) {
  const [draft, setDraft] = useState(value);
  const debounced = useDebouncedValue(draft, SEARCH_DEBOUNCE_MS);
  const lastCommitted = useRef(debounced);

  // Commits only when the typed text settles, never because `value` changed from outside;
  // otherwise clearing the filters elsewhere would be undone by the stale draft.
  useEffect(() => {
    if (debounced === lastCommitted.current) return;
    lastCommitted.current = debounced;
    if (debounced.trim() !== value) onCommit(debounced.trim());
  }, [debounced, value, onCommit]);

  // Follows outside changes (e.g. the sidebar link resetting the URL) without
  // clobbering text the user is still typing; adjusted during render, not in an effect.
  const [seenValue, setSeenValue] = useState(value);
  if (value !== seenValue) {
    setSeenValue(value);
    if (draft.trim() !== value) setDraft(value);
  }

  return (
    <div className="relative w-full md:max-w-md">
      <Label htmlFor={id} className="sr-only">
        Szukaj materiałów
      </Label>
      <Search
        className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
        aria-hidden="true"
      />
      <Input
        id={id}
        type="search"
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        placeholder="Szukaj po tytule, opisie lub wydarzeniu…"
        className="h-11 pl-9"
      />
    </div>
  );
}
