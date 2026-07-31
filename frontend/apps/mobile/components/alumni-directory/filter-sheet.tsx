import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { RangeSlider } from "@/components/ui/range-slider";
import { SearchMultiSelect, type SelectOption } from "@/components/ui/search-multi-select";
import { Text } from "@/components/ui/text";
import { EMPTY_FILTERS, YEAR_MAX, YEAR_MIN, type AlumniFilters } from "@/lib/alumni-directory";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Pressable, View } from "react-native";

type FilterSheetProps = {
  visible: boolean;
  value: AlumniFilters;
  skills: string[];
  companies: string[];
  onClose: () => void;
  onApply: (filters: AlumniFilters) => void;
};

// Skills and companies are identified by name, so the option id is the name.
const toOptions = (names: string[]): SelectOption[] =>
  names.map((name) => ({ id: name, label: name }));

// Shorter than the picker's own default: the dropdown scrolls inside the sheet's
// scroll view, and a tall one would swallow most of the sheet on open.
const SHEET_LIST_MAX_HEIGHT = 216;

function Eyebrow({ children }: { children: ReactNode }) {
  return (
    <Text className="text-muted-foreground text-[10px] font-semibold uppercase tracking-widest">
      {children}
    </Text>
  );
}

export function FilterSheet({
  visible,
  value,
  skills,
  companies,
  onClose,
  onApply,
}: FilterSheetProps) {
  const [draft, setDraft] = useState<AlumniFilters>(value);
  // The sheet keeps its children mounted while closed, so the pickers would
  // otherwise reopen still holding the previous search text and expanded list.
  // Bumping this on each open remounts them with fresh internal state.
  const [openCount, setOpenCount] = useState(0);

  // Seed the draft from the applied filters only on open — depending on `value`
  // too would wipe an in-progress edit if the parent re-created it mid-edit.
  useEffect(() => {
    if (visible) {
      setDraft(value);
      setOpenCount((n) => n + 1);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  const skillOptions = useMemo(() => toOptions(skills), [skills]);
  const companyOptions = useMemo(() => toOptions(companies), [companies]);

  return (
    <BottomSheet visible={visible} onClose={onClose} heightFraction={0.85}>
      <Text variant="h3" className="text-2xl font-bold">
        Filtruj
      </Text>

      <View className="mt-6 gap-3">
        <View className="flex-row items-center justify-between">
          <Eyebrow>Rok ukończenia</Eyebrow>
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

      {skillOptions.length > 0 ? (
        <View className="mt-6 gap-3">
          <Eyebrow>Umiejętności</Eyebrow>
          <SearchMultiSelect
            key={openCount}
            options={skillOptions}
            value={draft.skills}
            onChange={(next) =>
              setDraft((d) => ({
                ...d,
                skills: typeof next === "function" ? next(d.skills) : next,
              }))
            }
            placeholder="Wyszukaj umiejętności..."
            listMaxHeight={SHEET_LIST_MAX_HEIGHT}
            uppercaseChips
          />
        </View>
      ) : null}

      {companyOptions.length > 0 ? (
        <View className="mt-6 gap-3">
          <Eyebrow>Firma</Eyebrow>
          <SearchMultiSelect
            key={openCount}
            options={companyOptions}
            value={draft.companies}
            onChange={(next) =>
              setDraft((d) => ({
                ...d,
                companies: typeof next === "function" ? next(d.companies) : next,
              }))
            }
            placeholder="Wyszukaj firmy..."
            listMaxHeight={SHEET_LIST_MAX_HEIGHT}
          />
        </View>
      ) : null}

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
