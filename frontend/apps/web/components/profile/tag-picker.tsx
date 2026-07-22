"use client";

import { useMemo, useState } from "react";
import { X } from "lucide-react";
import type { TagResponse } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

const MAX_TAGS = 20;

type TagPickerProps = {
  availableTags: TagResponse[];
  selectedIds: string[];
  onChange: (ids: string[]) => void;
  disabled?: boolean;
};

export function TagPicker({ availableTags, selectedIds, onChange, disabled }: TagPickerProps) {
  const [query, setQuery] = useState("");

  const selected = useMemo(() => {
    const byId = new Map(availableTags.map((tag) => [tag.id, tag]));
    return selectedIds
      .map((id) => byId.get(id))
      .filter((tag): tag is TagResponse => tag != null);
  }, [availableTags, selectedIds]);

  const matches = useMemo(() => {
    const q = query.trim().toLocaleLowerCase("pl");
    if (!q) return [];

    return availableTags
      .filter((tag) => !selectedIds.includes(tag.id))
      .filter((tag) => tag.name.toLocaleLowerCase("pl").includes(q))
      .slice(0, 8);
  }, [availableTags, selectedIds, query]);

  function addTag(id: string) {
    if (disabled || selectedIds.includes(id) || selectedIds.length >= MAX_TAGS) return;
    onChange([...selectedIds, id]);
    setQuery("");
  }

  function removeTag(id: string) {
    if (disabled) return;
    onChange(selectedIds.filter((tagId) => tagId !== id));
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-3">
        <Label htmlFor="tag-search" className="text-xs font-bold tracking-wider uppercase">
          Umiejętności
        </Label>
        <span className="text-muted-foreground text-xs">
          {selectedIds.length}/{MAX_TAGS}
        </span>
      </div>

      {selected.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {selected.map((tag) => (
            <Badge
              key={tag.id}
              variant="secondary"
              className="bg-muted text-foreground gap-1 rounded-md px-2.5 py-1 text-xs font-semibold uppercase"
            >
              {tag.name}
              <button
                type="button"
                aria-label={`Usuń ${tag.name}`}
                disabled={disabled}
                onClick={() => removeTag(tag.id)}
                className="hover:text-destructive ml-0.5 inline-flex"
              >
                <X className="size-3" />
              </button>
            </Badge>
          ))}
        </div>
      ) : null}

      <div className="relative">
        <Input
          id="tag-search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Szukaj tagów, np. Java, Cloud…"
          disabled={disabled || selectedIds.length >= MAX_TAGS}
          autoComplete="off"
        />

        {matches.length > 0 ? (
          <ul
            role="listbox"
            className="border-border bg-background absolute z-10 mt-1 max-h-48 w-full overflow-y-auto rounded-lg border shadow-sm"
          >
            {matches.map((tag) => (
              <li key={tag.id}>
                <button
                  type="button"
                  role="option"
                  aria-selected={false}
                  className={cn(
                    "hover:bg-muted w-full px-3 py-2 text-left text-sm transition-colors",
                  )}
                  onClick={() => addTag(tag.id)}
                >
                  {tag.name}
                </button>
              </li>
            ))}
          </ul>
        ) : null}
      </div>

      {query.trim() && matches.length === 0 ? (
        <p className="text-muted-foreground text-xs">Brak tagów pasujących do „{query.trim()}”.</p>
      ) : null}
    </div>
  );
}
