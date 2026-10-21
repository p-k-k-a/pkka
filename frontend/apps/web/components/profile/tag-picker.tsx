"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Plus, X } from "lucide-react";
import { TAG_NAME_MAX_LENGTH, TAG_SUGGESTION_LIMIT } from "@pkka/domain";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverAnchor, PopoverContent } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

type TagPickerProps = {
  /** Most popular first: with an empty field only the first 20 are listed. */
  options: string[];
  value: string[];
  onChange: (names: string[]) => void;
  label: string;
  placeholder: string;
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
  allowCreate = false,
  disabled = false,
  reserveListSpace = false,
  format = (name) => name,
}: TagPickerProps) {
  const id = useId();
  const [query, setQuery] = useState("");
  const [isFocused, setIsFocused] = useState(false);
  const spaceRef = useRef<HTMLDivElement>(null);

  const searchText = query.trim();
  const searchLower = searchText.toLowerCase();
  const selectedKeys = new Set(value.map((name) => name.toLowerCase()));

  const available = options.filter((name) => !selectedKeys.has(name.toLowerCase()));
  const matches = searchLower
    ? available.filter((name) => name.toLowerCase().includes(searchLower))
    : available.slice(0, TAG_SUGGESTION_LIMIT);
  const exactMatch = options.find((name) => name.toLowerCase() === searchLower);
  const canCreate =
    allowCreate && searchLower !== "" && !exactMatch && !selectedKeys.has(searchLower);
  const items = canCreate ? [...matches, searchText] : matches;
  const isOpen = isFocused && items.length > 0;

  useEffect(() => {
    if (isOpen) spaceRef.current?.scrollIntoView({ block: "nearest" });
  }, [isOpen]);

  function add(name: string) {
    if (!disabled && !selectedKeys.has(name.toLowerCase())) onChange([...value, name]);
    setQuery("");
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key !== "Enter") return;
    event.preventDefault();
    if (exactMatch) add(exactMatch);
    else if (canCreate) add(searchText);
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
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setIsFocused(true);
              }}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              placeholder={placeholder}
              disabled={disabled}
              autoComplete="off"
              maxLength={TAG_NAME_MAX_LENGTH}
              onKeyDown={handleKeyDown}
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
          <ul>
            {items.map((name, index) => {
              const isCreate = canCreate && index === matches.length;
              return (
                <li key={isCreate ? "create" : name}>
                  <button
                    type="button"
                    className={cn(
                      "hover:bg-muted dark:hover:bg-foreground/10 flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm transition-colors",
                      isCreate && "font-semibold",
                    )}
                    onClick={() => add(name)}
                  >
                    {isCreate ? (
                      <>
                        <Plus className="size-4" />
                        Dodaj „{format(name)}”
                      </>
                    ) : (
                      format(name)
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        </PopoverContent>
      </Popover>

      {reserveListSpace ? <div ref={spaceRef} aria-hidden className="h-56" /> : null}
    </div>
  );
}
