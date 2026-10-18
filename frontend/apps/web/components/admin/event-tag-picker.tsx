"use client";

import { useState } from "react";
import { Plus, X } from "lucide-react";
import type { TagResponse } from "@pkka/api";
import { normalizeTagName, tagNameError, tagNameKey } from "@pkka/domain";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverAnchor, PopoverContent } from "@/components/ui/popover";

const MAX_SUGGESTIONS = 8;

type EventTagPickerProps = {
  catalog: TagResponse[];
  selected: string[];
  onChange: (names: string[]) => void;
};

export function EventTagPicker({ catalog, selected, onChange }: EventTagPickerProps) {
  const [query, setQuery] = useState("");
  const [isFocused, setIsFocused] = useState(false);

  const typed = tagNameKey(normalizeTagName(query));
  const available = catalog.map((tag) => tag.name).filter((name) => !selected.includes(name));
  const matches = (
    typed ? available.filter((name) => name.includes(typed)) : [...available].sort()
  ).slice(0, MAX_SUGGESTIONS);
  const isNew = typed !== "" && !selected.includes(typed) && !available.includes(typed);
  const problem = isNew ? tagNameError(typed) : null;
  const canCreate = isNew && problem === null;
  const isOpen = isFocused && (matches.length > 0 || canCreate);

  function add(name: string) {
    if (!selected.includes(name)) onChange([...selected, name]);
    setQuery("");
  }

  return (
    <div className="flex flex-col gap-3">
      {selected.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {selected.map((name) => (
            <Badge
              key={name}
              variant="secondary"
              className="bg-muted text-foreground h-auto gap-1 rounded-md px-2.5 py-1 text-xs font-semibold"
            >
              #{name}
              <button
                type="button"
                aria-label={`Usuń ${name}`}
                onClick={() => onChange(selected.filter((tag) => tag !== name))}
                className="hover:text-destructive ml-0.5 inline-flex"
              >
                <X className="size-3" />
              </button>
            </Badge>
          ))}
        </div>
      ) : null}

      <Popover open={isOpen} onOpenChange={(next) => !next && setIsFocused(false)}>
        <PopoverAnchor asChild>
          <div className="relative">
            <Label htmlFor="event-tag-search" className="sr-only">
              Szukaj lub dodaj tag
            </Label>
            <Input
              id="event-tag-search"
              role="combobox"
              aria-expanded={isOpen}
              aria-controls="event-tag-results"
              aria-autocomplete="list"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              placeholder="Szukaj lub dodaj tag, np. warsztat…"
              autoComplete="off"
              onKeyDown={(event) => {
                if (event.key !== "Enter") return;
                event.preventDefault();
                if (typed === "" || problem) return;
                add(available.includes(typed) || canCreate ? typed : (matches[0] ?? typed));
              }}
            />
          </div>
        </PopoverAnchor>

        <PopoverContent
          align="start"
          className="max-h-64 w-(--radix-popover-trigger-width) gap-0 overflow-y-auto p-1"
          onOpenAutoFocus={(event) => event.preventDefault()}
        >
          <ul id="event-tag-results" role="listbox" aria-label="Pasujące tagi">
            {matches.map((name) => (
              <li key={name}>
                <button
                  type="button"
                  role="option"
                  aria-selected={false}
                  className="hover:bg-muted dark:hover:bg-foreground/10 w-full rounded-md px-3 py-2 text-left text-sm transition-colors"
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => add(name)}
                >
                  #{name}
                </button>
              </li>
            ))}
            {canCreate ? (
              <li>
                <button
                  type="button"
                  role="option"
                  aria-selected={false}
                  className="hover:bg-muted dark:hover:bg-foreground/10 flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm font-semibold transition-colors"
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => add(typed)}
                >
                  <Plus className="size-4" />
                  Dodaj #{typed}
                </button>
              </li>
            ) : null}
          </ul>
        </PopoverContent>
      </Popover>

      {problem ? <p className="text-muted-foreground text-[13px]">{problem}</p> : null}
    </div>
  );
}
