import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { THEME } from "@/lib/theme";
import { cn } from "@/lib/utils";
import { ChevronDown, ChevronUp, Search, X } from "lucide-react-native";
import { useMemo, useRef, useState } from "react";
import { Keyboard, Pressable, ScrollView, TextInput, View } from "react-native";

// Caps the open dropdown so a long option list stays scrollable instead of
// pushing the rest of the form (or sheet) off-screen.
const LIST_MAX_HEIGHT = 288;

export type SelectOption = {
  id: string;
  label: string;
};

type SearchMultiSelectProps = {
  options: SelectOption[];
  /** Selected option ids. */
  value: string[];
  onChange: (next: string[] | ((prev: string[]) => string[])) => void;
  placeholder: string;
  /** Upper bound on selections; omit for unlimited. */
  max?: number;
  /** Shown once `max` selections are reached. */
  maxHint?: string;
  emptyText?: string;
  listMaxHeight?: number;
  /** Uppercase the selected chips, as the profile form does for skills. */
  uppercaseChips?: boolean;
};

/**
 * Searchable multi-select: selected values as removable chips above a search
 * field that opens a scrollable, checkable option list. Used wherever the option
 * set can grow past what a flat wall of chips can show — skills, companies.
 */
export function SearchMultiSelect({
  options,
  value,
  onChange,
  placeholder,
  max,
  maxHint,
  emptyText = "Brak wyników.",
  listMaxHeight = LIST_MAX_HEIGHT,
  uppercaseChips = false,
}: SearchMultiSelectProps) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const inputRef = useRef<TextInput>(null);

  const selected = useMemo(
    () => options.filter((option) => value.includes(option.id)),
    [options, value],
  );

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return needle
      ? options.filter((option) => option.label.toLowerCase().includes(needle))
      : options;
  }, [options, query]);

  const atMax = max !== undefined && value.length >= max;

  // Compute from `prev` (the freshest value) rather than the captured `value`
  // prop, so a memoized onPress closure can never act on a stale snapshot.
  const toggle = (id: string) => {
    onChange((prev) => {
      if (prev.includes(id)) return prev.filter((optionId) => optionId !== id);
      if (max !== undefined && prev.length >= max) return prev;
      return [...prev, id];
    });
  };

  // Collapse resets the search so the field shows a clean placeholder again.
  const close = () => {
    setOpen(false);
    setQuery("");
    Keyboard.dismiss();
  };

  return (
    <View className="gap-3">
      {selected.length > 0 ? (
        <View className="flex-row flex-wrap gap-2">
          {selected.map((option) => (
            <Pressable
              key={option.id}
              onPress={() => toggle(option.id)}
              accessibilityLabel={`Usuń: ${option.label}`}
              className="bg-muted flex-row items-center gap-1 rounded-md px-3 py-1.5 active:opacity-70"
            >
              <Text className="text-foreground text-xs font-semibold">
                {uppercaseChips ? option.label.toUpperCase() : option.label}
              </Text>
              <X size={12} color={THEME.light.mutedForeground} />
            </Pressable>
          ))}
        </View>
      ) : null}

      <View className="relative justify-center">
        <View pointerEvents="none" className="absolute left-3 z-10">
          <Search size={18} color={THEME.light.mutedForeground} />
        </View>
        <Input
          ref={inputRef}
          value={query}
          onChangeText={setQuery}
          onFocus={() => setOpen(true)}
          onBlur={close}
          placeholder={placeholder}
          autoCapitalize="none"
          autoCorrect={false}
          className="pl-10 pr-10"
        />
        <Pressable
          onPress={() => {
            if (query) {
              setQuery("");
              inputRef.current?.focus();
            } else if (open) {
              close();
            } else {
              inputRef.current?.focus();
            }
          }}
          accessibilityLabel={
            query ? "Wyczyść wyszukiwanie" : open ? "Zamknij listę" : "Otwórz listę"
          }
          className="absolute right-2 h-8 w-8 items-center justify-center rounded-full active:bg-muted"
        >
          {query ? (
            <X size={18} color={THEME.light.mutedForeground} />
          ) : open ? (
            <ChevronUp size={18} color={THEME.light.mutedForeground} />
          ) : (
            <ChevronDown size={18} color={THEME.light.mutedForeground} />
          )}
        </Pressable>
      </View>

      {atMax && maxHint ? <Text className="text-muted-foreground text-xs">{maxHint}</Text> : null}

      {open ? (
        filtered.length === 0 ? (
          <View className="border-border rounded-md border px-3 py-4">
            <Text className="text-muted-foreground text-sm">{emptyText}</Text>
          </View>
        ) : (
          <View className="border-border overflow-hidden rounded-md border">
            <ScrollView
              style={{ maxHeight: listMaxHeight }}
              keyboardShouldPersistTaps="handled"
              nestedScrollEnabled
            >
              {filtered.map((option, index) => {
                const isSelected = value.includes(option.id);
                const disabled = atMax && !isSelected;
                return (
                  <Pressable
                    key={option.id}
                    onPress={() => toggle(option.id)}
                    disabled={disabled}
                    accessibilityRole="checkbox"
                    accessibilityState={{ checked: isSelected, disabled }}
                    className={cn(
                      "flex-row items-center gap-3 px-3 py-3",
                      index > 0 && "border-border border-t",
                      isSelected && "bg-muted/40",
                      disabled && "opacity-40",
                    )}
                  >
                    <View pointerEvents="none">
                      <Checkbox checked={isSelected} onCheckedChange={() => {}} />
                    </View>
                    <Text className="text-foreground flex-1 text-sm">{option.label}</Text>
                  </Pressable>
                );
              })}
            </ScrollView>
          </View>
        )
      ) : null}
    </View>
  );
}
