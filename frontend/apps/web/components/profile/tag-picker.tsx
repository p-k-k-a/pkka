"use client";

import { useMemo, useState } from "react";
import { Plus, X } from "lucide-react";
import { normalizeTagName } from "@pkka/domain";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverAnchor, PopoverContent } from "@/components/ui/popover";

export type TagOption = {
  id: string;
  label: string;
};

type TagPickerProps = {
  /** Most popular first: with an empty field only the first `idleLimit` are listed. */
  options: TagOption[];
  selectedIds: string[];
  onChange: (ids: string[]) => void;
  /** Caps the selection and shows a counter; leave out for an unlimited picker. */
  max?: number;
  idleLimit?: number;
  disabled?: boolean;
  onCreate?: (label: string) => void;
  createError?: (label: string) => string | null;
  placeholder?: string;
};

export function TagPicker({
  options,
  selectedIds,
  onChange,
  max,
  idleLimit,
  disabled,
  onCreate,
  createError,
  placeholder = "Wyszukaj umiejętności…",
}: TagPickerProps) {
  const [query, setQuery] = useState("");
  const [isFocused, setIsFocused] = useState(false);

  const selected = useMemo(
    () => selectedIds.flatMap((id) => options.filter((option) => option.id === id)),
    [options, selectedIds],
  );

  const typed = normalizeTagName(query);
  const needle = typed.toLocaleLowerCase("pl");
  const isFull = max !== undefined && selectedIds.length >= max;

  const available = options.filter((option) => !selectedIds.includes(option.id));
  const matches = needle
    ? available.filter((option) => option.label.toLocaleLowerCase("pl").includes(needle))
    : idleLimit === undefined
      ? available
      : available.slice(0, idleLimit);
  const hasMoreWhenIdle = !needle && matches.length < available.length;

  const canCreate =
    onCreate !== undefined &&
    needle !== "" &&
    !options.some((option) => option.label.toLocaleLowerCase("pl") === needle);
  const createProblem = canCreate ? (createError?.(typed) ?? null) : null;

  const isOpen = isFocused && !isFull && (matches.length > 0 || canCreate);

  function addTag(id: string) {
    if (disabled || isFull || selectedIds.includes(id)) return;
    onChange([...selectedIds, id]);
    setQuery("");
  }

  function createTag() {
    if (disabled || isFull || !canCreate || createProblem) return;
    onCreate?.(typed);
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
              {tag.label}
              <button
                type="button"
                aria-label={`Usuń ${tag.label}`}
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
                isOpen && needle && matches[0] ? `tag-search-option-${matches[0].id}` : undefined
              }
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              placeholder={placeholder}
              disabled={disabled || isFull}
              autoComplete="off"
              onKeyDown={(event) => {
                // The picker lives inside a form — Enter picks the exact or first
                // match, or creates the tag, instead of submitting the form.
                if (event.key !== "Enter") return;
                event.preventDefault();
                if (!needle) return;
                const exact = available.find(
                  (option) => option.label.toLocaleLowerCase("pl") === needle,
                );
                if (exact) addTag(exact.id);
                else if (canCreate) createTag();
                else if (matches[0]) addTag(matches[0].id);
              }}
            />
          </div>
        </PopoverAnchor>

        <PopoverContent
          align="start"
          className="max-h-72 w-(--radix-popover-trigger-width) gap-0 overflow-y-auto p-1"
          onOpenAutoFocus={(event) => event.preventDefault()}
        >
          <ul id="tag-search-results" role="listbox" aria-label="Pasujące umiejętności">
            {matches.map((tag) => (
              <li key={tag.id}>
                <button
                  id={`tag-search-option-${tag.id}`}
                  type="button"
                  role="option"
                  aria-selected={false}
                  className="hover:bg-muted dark:hover:bg-foreground/10 w-full rounded-md px-3 py-2 text-left text-sm transition-colors"
                  // Keeps the input focused so the list survives the click.
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => addTag(tag.id)}
                >
                  {tag.label}
                </button>
              </li>
            ))}
            {canCreate && createProblem ? (
              <li className="text-muted-foreground px-3 py-2 text-sm">{createProblem}</li>
            ) : canCreate ? (
              <li>
                <button
                  type="button"
                  role="option"
                  aria-selected={false}
                  className="hover:bg-muted dark:hover:bg-foreground/10 flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm font-semibold transition-colors"
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={createTag}
                >
                  <Plus className="size-4" />
                  Dodaj „{typed}”
                </button>
              </li>
            ) : null}
            {hasMoreWhenIdle ? (
              <li className="text-muted-foreground px-3 py-2 text-xs">
                Wpisz nazwę, aby znaleźć pozostałe.
              </li>
            ) : null}
          </ul>
        </PopoverContent>
      </Popover>

      <div className="flex items-center justify-between gap-3">
        {needle && matches.length === 0 && !canCreate ? (
          <p className="text-muted-foreground text-[13px]">Brak tagów pasujących do „{typed}”.</p>
        ) : (
          <span />
        )}
        {max !== undefined ? (
          <span className="text-muted-foreground text-[13px]">
            {selectedIds.length}/{max}
          </span>
        ) : null}
      </div>
    </div>
  );
}
