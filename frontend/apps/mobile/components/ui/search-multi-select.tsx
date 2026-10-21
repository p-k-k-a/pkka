import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { cn } from "@/lib/utils";
import { ChevronDown, ChevronUp, Plus, Search, X } from "lucide-react-native";
import { TAG_SUGGESTION_LIMIT } from "@pkka/domain";
import { useRef, useState } from "react";
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
  emptyText?: string;
  listMaxHeight?: number;
  /** Uppercase the selected chips, as the profile form does for skills. */
  uppercaseChips?: boolean;
  onCreate?: (label: string) => void;
  maxLength?: number;
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
  emptyText = "Brak wyników.",
  listMaxHeight = LIST_MAX_HEIGHT,
  uppercaseChips = false,
  onCreate,
  maxLength,
}: SearchMultiSelectProps) {
  const theme = useThemeColors();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const inputRef = useRef<TextInput>(null);

  const selected = options.filter((option) => value.includes(option.id));

  const searchText = query.trim();
  const searchLower = searchText.toLowerCase();
  const exactMatch = options.find((option) => option.label.toLowerCase() === searchLower);

  const filtered = searchLower
    ? options.filter((option) => option.label.toLowerCase().includes(searchLower))
    : options.slice(0, TAG_SUGGESTION_LIMIT);

  const canCreate = onCreate !== undefined && searchLower !== "" && !exactMatch;

  // Compute from `prev` (the freshest value) rather than the captured `value`
  // prop, so a memoized onPress closure can never act on a stale snapshot.
  const toggle = (id: string) => {
    onChange((prev) => {
      if (prev.includes(id)) return prev.filter((optionId) => optionId !== id);
      return [...prev, id];
    });
  };

  const submit = () => {
    if (exactMatch && !value.includes(exactMatch.id)) toggle(exactMatch.id);
    else if (canCreate) onCreate(searchText);
    setQuery("");
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
              <X size={12} color={theme.mutedForeground} />
            </Pressable>
          ))}
        </View>
      ) : null}

      <View className="relative justify-center">
        <View pointerEvents="none" className="absolute left-3 z-10">
          <Search size={18} color={theme.mutedForeground} />
        </View>
        <Input
          ref={inputRef}
          value={query}
          onChangeText={setQuery}
          onFocus={() => setOpen(true)}
          onBlur={close}
          submitBehavior="submit"
          onSubmitEditing={submit}
          maxLength={maxLength}
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
            <X size={18} color={theme.mutedForeground} />
          ) : open ? (
            <ChevronUp size={18} color={theme.mutedForeground} />
          ) : (
            <ChevronDown size={18} color={theme.mutedForeground} />
          )}
        </Pressable>
      </View>

      {open ? (
        filtered.length === 0 && !canCreate ? (
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
                return (
                  <Pressable
                    key={option.id}
                    onPress={() => toggle(option.id)}
                    accessibilityRole="checkbox"
                    accessibilityState={{ checked: isSelected }}
                    className={cn(
                      "flex-row items-center gap-3 px-3 py-3",
                      index > 0 && "border-border border-t",
                      isSelected && "bg-muted/40",
                    )}
                  >
                    <View pointerEvents="none">
                      <Checkbox checked={isSelected} onCheckedChange={() => {}} />
                    </View>
                    <Text className="text-foreground flex-1 text-sm">{option.label}</Text>
                  </Pressable>
                );
              })}
              {canCreate ? (
                <Pressable
                  onPress={submit}
                  accessibilityRole="button"
                  accessibilityLabel={`Dodaj: ${searchText}`}
                  className={cn(
                    "flex-row items-center gap-3 px-3 py-3 active:bg-muted",
                    filtered.length > 0 && "border-border border-t",
                  )}
                >
                  <Plus size={18} color={theme.primary} />
                  <Text className="text-foreground flex-1 text-sm font-semibold">
                    Dodaj „{searchText}”
                  </Text>
                </Pressable>
              ) : null}
              {!searchLower && options.length > TAG_SUGGESTION_LIMIT ? (
                <View className="border-border border-t px-3 py-3">
                  <Text className="text-muted-foreground text-xs">
                    Wpisz nazwę, aby znaleźć pozostałe.
                  </Text>
                </View>
              ) : null}
            </ScrollView>
          </View>
        )
      ) : null}
    </View>
  );
}
