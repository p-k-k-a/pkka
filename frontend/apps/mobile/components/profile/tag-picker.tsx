import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { THEME } from "@/lib/theme";
import { cn } from "@/lib/utils";
import type { TagResponse } from "@pkka/api";
import { ChevronDown, ChevronUp, Search, X } from "lucide-react-native";
import { useMemo, useRef, useState } from "react";
import { Keyboard, Pressable, ScrollView, TextInput, View } from "react-native";

const MAX_TAGS = 20;
// Caps the open dropdown so a long tag list stays scrollable instead of pushing
// the rest of the form off-screen.
const LIST_MAX_HEIGHT = 288;

type TagPickerProps = {
  options: TagResponse[];
  value: string[];
  onChange: (ids: string[] | ((prev: string[]) => string[])) => void;
  loading?: boolean;
  error?: boolean;
  max?: number;
};

export function TagPicker({
  options,
  value,
  onChange,
  loading = false,
  error = false,
  max = MAX_TAGS,
}: TagPickerProps) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const inputRef = useRef<TextInput>(null);

  const selected = useMemo(() => options.filter((tag) => value.includes(tag.id)), [options, value]);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return needle ? options.filter((tag) => tag.name.toLowerCase().includes(needle)) : options;
  }, [options, query]);

  const atMax = value.length >= max;

  // Compute from `prev` (the freshest form value) rather than the captured
  // `value` prop, so a memoized onPress closure can never act on a stale
  // snapshot.
  const toggle = (id: string) => {
    onChange((prev) => {
      if (prev.includes(id)) return prev.filter((tagId) => tagId !== id);
      if (prev.length >= max) return prev;
      return [...prev, id];
    });
  };

  // Collapse resets the search so the field shows a clean placeholder again.
  const close = () => {
    setOpen(false);
    setQuery("");
    Keyboard.dismiss();
  };

  if (loading) {
    return <Text className="text-muted-foreground text-sm">Ładowanie umiejętności...</Text>;
  }

  if (error) {
    return (
      <Text className="text-destructive text-sm font-medium">
        Nie udało się załadować listy umiejętności. Spróbuj ponownie później.
      </Text>
    );
  }

  return (
    <View className="gap-3">
      {selected.length > 0 ? (
        <View className="flex-row flex-wrap gap-2">
          {selected.map((tag) => (
            <Pressable
              key={tag.id}
              onPress={() => toggle(tag.id)}
              accessibilityLabel={`Usuń: ${tag.name}`}
              className="bg-muted flex-row items-center gap-1 rounded-md px-3 py-1.5 active:opacity-70"
            >
              <Text className="text-foreground text-xs font-semibold">
                {tag.name.toUpperCase()}
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
          placeholder="Wyszukaj umiejętności..."
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
            query
              ? "Wyczyść wyszukiwanie"
              : open
                ? "Zamknij listę umiejętności"
                : "Otwórz listę umiejętności"
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

      {atMax ? (
        <Text className="text-muted-foreground text-xs">
          Możesz wybrać maksymalnie {max} umiejętności.
        </Text>
      ) : null}

      {open ? (
        filtered.length === 0 ? (
          <View className="border-border rounded-md border px-3 py-4">
            <Text className="text-muted-foreground text-sm">Brak wyników.</Text>
          </View>
        ) : (
          <View className="border-border overflow-hidden rounded-md border">
            <ScrollView
              style={{ maxHeight: LIST_MAX_HEIGHT }}
              keyboardShouldPersistTaps="handled"
              nestedScrollEnabled
            >
              {filtered.map((tag, index) => {
                const isSelected = value.includes(tag.id);
                const disabled = atMax && !isSelected;
                return (
                  <Pressable
                    key={tag.id}
                    onPress={() => toggle(tag.id)}
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
                    <Text className="text-foreground flex-1 text-sm">{tag.name}</Text>
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
