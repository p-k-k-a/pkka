import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { RangeSlider } from "@/components/ui/range-slider";
import { SearchMultiSelect, type SelectOption } from "@/components/ui/search-multi-select";
import { Text } from "@/components/ui/text";
import { EMPTY_FILTERS, YEAR_MAX, YEAR_MIN, type AlumniFilters } from "@/lib/alumni-directory";
import { cn } from "@/lib/utils";
import { useListUserTags } from "@pkka/api";
import { Check } from "lucide-react-native";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Pressable, View } from "react-native";

type FilterSheetProps = {
  visible: boolean;
  value: AlumniFilters;
  onClose: () => void;
  onApply: (filters: AlumniFilters) => void;
};

const SHEET_LIST_MAX_HEIGHT = 216;

function HeaderText({ children }: { children: ReactNode }) {
  return (
    <Text className="text-muted-foreground text-[10px] font-semibold uppercase tracking-widest">
      {children}
    </Text>
  );
}

export function FilterSheet({ visible, value, onClose, onApply }: FilterSheetProps) {
  const [draft, setDraft] = useState<AlumniFilters>(value);
  // The sheet keeps its children mounted while closed, so the pickers would
  // otherwise reopen still holding the previous search text and expanded list.
  // Bumping this on each open remounts them with fresh internal state.
  const [openCount, setOpenCount] = useState(0);

  const tagsQuery = useListUserTags();
  const tagOptions = useMemo<SelectOption[]>(
    () => (tagsQuery.data?.data ?? []).map((tag) => ({ id: tag.id, label: tag.name })),
    [tagsQuery.data],
  );

  useEffect(() => {
    if (visible) {
      setDraft(value);
      setOpenCount((n) => n + 1);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  return (
    <BottomSheet visible={visible} onClose={onClose} heightFraction={0.6}>
      <Text variant="h3" className="text-2xl font-bold">
        Filtruj
      </Text>

      <View className="mt-6 gap-3">
        <View className="flex-row items-center justify-between">
          <HeaderText>Rok ukończenia</HeaderText>
          <Text className="text-foreground text-sm font-semibold">
            {draft.yearRange[0]} — {draft.yearRange[1]}
          </Text>
        </View>
        <RangeSlider
          min={YEAR_MIN}
          max={YEAR_MAX}
          low={draft.yearRange[0]}
          high={draft.yearRange[1]}
          onChange={(low, high) => setDraft((d) => ({ ...d, yearRange: [low, high] }))}
        />
        <View className="flex-row justify-between">
          <Text className="text-muted-foreground text-xs">{YEAR_MIN}</Text>
          <Text className="text-muted-foreground text-xs">{YEAR_MAX}</Text>
        </View>
      </View>

      <View className="mt-6 gap-3">
        <HeaderText>Umiejętności</HeaderText>
        {tagsQuery.isPending ? (
          <Text className="text-muted-foreground text-sm">Ładowanie umiejętności...</Text>
        ) : tagsQuery.isError ? (
          <Text className="text-destructive text-sm font-medium">
            Nie udało się załadować listy umiejętności.
          </Text>
        ) : (
          <SearchMultiSelect
            key={openCount}
            options={tagOptions}
            value={draft.tagIds}
            onChange={(next) =>
              setDraft((d) => ({
                ...d,
                tagIds: typeof next === "function" ? next(d.tagIds) : next,
              }))
            }
            placeholder="Wyszukaj umiejętności..."
            listMaxHeight={SHEET_LIST_MAX_HEIGHT}
            uppercaseChips
          />
        )}
      </View>

      <View className="mt-6 gap-3">
        <HeaderText>Mentoring</HeaderText>
        <Pressable
          role="checkbox"
          accessibilityState={{ checked: draft.mentorOnly }}
          accessibilityLabel="Tylko chętni do mentoringu"
          onPress={() => setDraft((d) => ({ ...d, mentorOnly: !d.mentorOnly }))}
          className="flex-row items-center gap-3 py-1"
        >
          <View
            className={cn(
              "size-5 items-center justify-center rounded border-2",
              draft.mentorOnly ? "border-primary bg-primary" : "border-muted-foreground/40",
            )}
          >
            {draft.mentorOnly ? <Check size={14} color="white" strokeWidth={3} /> : null}
          </View>
          <Text className="text-foreground text-base">Tylko chętni do mentoringu</Text>
        </Pressable>
      </View>

      <View className="mt-6 gap-3">
        <Button size="lg" onPress={() => onApply(draft)}>
          <Text className="font-bold">Zastosuj filtry</Text>
        </Button>
        <Pressable onPress={() => setDraft(EMPTY_FILTERS)} className="items-center py-1">
          <Text className="text-muted-foreground text-sm font-semibold">Wyczyść wszystko</Text>
        </Pressable>
      </View>
    </BottomSheet>
  );
}
