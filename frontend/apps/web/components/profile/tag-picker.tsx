"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Plus, X } from "lucide-react";
import { TAG_NAME_MAX_LENGTH } from "@pkka/domain";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverAnchor, PopoverContent } from "@/components/ui/popover";

const IDLE_LIMIT = 20;

type TagPickerProps = {
  /** Most popular first: with an empty field only the first 20 are listed. */
  options: string[];
  value: string[];
  onChange: (names: string[]) => void;
  label: string;
  placeholder: string;
  max?: number;
  allowCreate?: boolean;
  disabled?: boolean;
  /** Keeps room below the field so the suggestions fit even at the bottom of a page. */
  reserveListSpace?: boolean;
  format?: (name: string) => string;
};

export function TagPicker({
  options,
  value,
  onChange,
  label,
  placeholder,
  max,
  allowCreate = false,
  disabled = false,
  reserveListSpace = false,
  format = (name) => name,
}: TagPickerProps) {
  const id = useId();
  const [query, setQuery] = useState("");
  const [isFocused, setIsFocused] = useState(false);
  const spaceRef = useRef<HTMLDivElement>(null);

  const typed = query.trim();
  const key = typed.toLowerCase();
  const selectedKeys = new Set(value.map((name) => name.toLowerCase()));
  const isFull = max !== undefined && value.length >= max;

  const available = options.filter((name) => !selectedKeys.has(name.toLowerCase()));
  const matches = key
    ? available.filter((name) => name.toLowerCase().includes(key))
    : available.slice(0, IDLE_LIMIT);
  const exact = options.find((name) => name.toLowerCase() === key);
  const canCreate = allowCreate && key !== "" && !exact && !selectedKeys.has(key);
  const isOpen = isFocused && !isFull && (matches.length > 0 || canCreate);

  useEffect(() => {
    if (isOpen) spaceRef.current?.scrollIntoView({ block: "nearest" });
  }, [isOpen]);

  function add(name: string) {
    if (!disabled && !isFull && !selectedKeys.has(name.toLowerCase())) onChange([...value, name]);
    setQuery("");
  }

  return (
    <div className="flex flex-col gap-3">
      {value.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {value.map((name) => (
            <Badge
              key={name}
              variant="secondary"
              className="bg-muted text-foreground h-auto gap-1 rounded-md px-2.5 py-1 text-xs font-semibold"
            >
              {format(name)}
              <button
                type="button"
                aria-label={`Usuń ${name}`}
                disabled={disabled}
                onClick={() => onChange(value.filter((tag) => tag !== name))}
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
            <Label htmlFor={id} className="sr-only">
              {label}
            </Label>
            <Input
              id={id}
              role="combobox"
              aria-expanded={isOpen}
              aria-controls={`${id}-results`}
              aria-autocomplete="list"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setIsFocused(true);
              }}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              placeholder={placeholder}
              disabled={disabled || isFull}
              autoComplete="off"
              maxLength={TAG_NAME_MAX_LENGTH}
              onKeyDown={(event) => {
                if (event.key !== "Enter") return;
                event.preventDefault();
                if (exact) add(exact);
                else if (canCreate) add(typed);
              }}
            />
          </div>
        </PopoverAnchor>

        <PopoverContent
          align="start"
          side="bottom"
          avoidCollisions={false}
          className="max-h-[min(14rem,var(--radix-popover-content-available-height))] w-(--radix-popover-trigger-width) gap-0 overflow-y-auto p-1"
          onOpenAutoFocus={(event) => event.preventDefault()}
          onMouseDown={(event) => event.preventDefault()}
        >
          <ul id={`${id}-results`} role="listbox" aria-label={label}>
            {matches.map((name) => (
              <li key={name} role="presentation">
                <button
                  type="button"
                  role="option"
                  aria-selected={false}
                  className="hover:bg-muted dark:hover:bg-foreground/10 w-full rounded-md px-3 py-2 text-left text-sm transition-colors"
                  onClick={() => add(name)}
                >
                  {format(name)}
                </button>
              </li>
            ))}
            {canCreate ? (
              <li role="presentation">
                <button
                  type="button"
                  role="option"
                  aria-selected={false}
                  className="hover:bg-muted dark:hover:bg-foreground/10 flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm font-semibold transition-colors"
                  onClick={() => add(typed)}
                >
                  <Plus className="size-4" />
                  Dodaj „{format(typed)}”
                </button>
              </li>
            ) : null}
          </ul>
        </PopoverContent>
      </Popover>

      {max !== undefined ? (
        <span className="text-muted-foreground self-end text-[13px]">
          {value.length}/{max}
        </span>
      ) : null}

      {reserveListSpace ? <div ref={spaceRef} aria-hidden className="h-56" /> : null}
    </div>
  );
}
