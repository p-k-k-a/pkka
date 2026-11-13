"use client";

import { useEffect, useRef, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useDebouncedValue } from "@/lib/use-debounced-value";

const SEARCH_DEBOUNCE_MS = 300;

type MaterialSearchInputProps = {
  id: string;
  value: string;
  onCommit: (query: string) => void;
};

export function MaterialSearchInput({ id, value, onCommit }: MaterialSearchInputProps) {
  const [draft, setDraft] = useState(value);
  const debounced = useDebouncedValue(draft, SEARCH_DEBOUNCE_MS);
  const lastCommitted = useRef(debounced);

  // Commits only when the typed text settles, not when `value` changes from outside,
  // or a stale draft would undo filters cleared elsewhere.
  useEffect(() => {
    if (debounced === lastCommitted.current) return;
    lastCommitted.current = debounced;
    if (debounced.trim() !== value) onCommit(debounced.trim());
  }, [debounced, value, onCommit]);

  // Follows outside changes (e.g. the sidebar link) without clobbering text being typed.
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
