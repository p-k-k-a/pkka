"use client";

import { useMemo, useState } from "react";
import { X } from "lucide-react";
import type { UserTagResponse } from "@pkka/api";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverAnchor, PopoverContent } from "@/components/ui/popover";

const MAX_TAGS = 20;

type TagPickerProps = {
  availableTags: UserTagResponse[];
  selectedIds: string[];
  onChange: (ids: string[]) => void;
  disabled?: boolean;
};

export function TagPicker({ availableTags, selectedIds, onChange, disabled }: TagPickerProps) {
  const [query, setQuery] = useState("");
  const [isFocused, setIsFocused] = useState(false);

  const selected = useMemo(() => {
    const byId = new Map(availableTags.map((tag) => [tag.id, tag]));
    return selectedIds
      .map((id) => byId.get(id))
      .filter((tag): tag is UserTagResponse => tag != null);
  }, [availableTags, selectedIds]);

  const matches = useMemo(() => {
    const q = query.trim().toLocaleLowerCase("pl");
    if (!q) return [];

    return availableTags
      .filter((tag) => !selectedIds.includes(tag.id))
      .filter((tag) => tag.name.toLocaleLowerCase("pl").includes(q))
      .slice(0, 8);
  }, [availableTags, selectedIds, query]);

  const isOpen = isFocused && matches.length > 0;

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
      {selected.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {selected.map((tag) => (
            <Badge
              key={tag.id}
              variant="secondary"
              className="bg-muted text-foreground h-auto gap-1 rounded-md px-2.5 py-1 text-xs font-semibold uppercase"
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

      {/* The suggestions render in a portal so the surrounding card, which clips
          its content, cannot cut the list off. */}
      <Popover open={isOpen} onOpenChange={(next) => !next && setIsFocused(false)}>
        <PopoverAnchor asChild>
          {/* Positioned so the sr-only label below resolves against this wrapper
              instead of escaping the dashboard's scroll container. */}
          <div className="relative">
            <Label htmlFor="tag-search" className="sr-only">
              Szukaj umiejętności
            </Label>
            <Input
              id="tag-search"
              role="combobox"
              aria-expanded={isOpen}
              aria-controls="tag-search-results"
              aria-autocomplete="list"
              aria-activedescendant={
                isOpen && matches[0] ? `tag-search-option-${matches[0].id}` : undefined
              }
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              placeholder="Szukaj tagów, np. Java, Cloud…"
              disabled={disabled || selectedIds.length >= MAX_TAGS}
              autoComplete="off"
              onKeyDown={(event) => {
                // The picker lives inside a form — Enter picks the first match
                // instead of submitting a half-filled form.
                if (event.key !== "Enter") return;
                event.preventDefault();
                if (matches[0]) addTag(matches[0].id);
              }}
            />
          </div>
        </PopoverAnchor>

        <PopoverContent
          align="start"
          className="max-h-48 w-(--radix-popover-trigger-width) gap-0 overflow-y-auto p-1"
          onOpenAutoFocus={(event) => event.preventDefault()}
        >
          <ul id="tag-search-results" role="listbox" aria-label="Pasujące umiejętności">
            {matches.map((tag) => (
              <li key={tag.id}>
                <button
                  id={`tag-search-option-${tag.id}`}
                  type="button"
                  role="option"
                  aria-selected={matches[0]?.id === tag.id}
                  className="hover:bg-muted w-full rounded-md px-3 py-2 text-left text-sm transition-colors"
                  // Keeps the input focused so the list survives the click.
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => addTag(tag.id)}
                >
                  {tag.name}
                </button>
              </li>
            ))}
          </ul>
        </PopoverContent>
      </Popover>

      <div className="flex items-center justify-between gap-3">
        {query.trim() && matches.length === 0 ? (
          <p className="text-muted-foreground text-[13px]">
            Brak tagów pasujących do „{query.trim()}”.
          </p>
        ) : (
          <span />
        )}
        <span className="text-muted-foreground text-[13px]">
          {selectedIds.length}/{MAX_TAGS}
        </span>
      </div>
    </div>
  );
}
