import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { RangeSlider } from "@/components/ui/range-slider";
import { Text } from "@/components/ui/text";
import { EMPTY_FILTERS, YEAR_MAX, YEAR_MIN, type AlumniFilters } from "@/lib/alumni-directory";
import { cn } from "@/lib/utils";
import * as React from "react";
import { Pressable, View } from "react-native";

type FilterSheetProps = {
  visible: boolean;
  value: AlumniFilters;
  skills: string[];
  companies: string[];
  onClose: () => void;
  onApply: (filters: AlumniFilters) => void;
};

function FilterChip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      role="checkbox"
      accessibilityState={{ checked: selected }}
      className={cn(
        "rounded-md border px-4 py-2",
        selected ? "border-foreground bg-foreground" : "border-border bg-background",
      )}
    >
      <Text className={cn("text-sm font-medium", selected ? "text-background" : "text-foreground")}>
        {label}
      </Text>
    </Pressable>
  );
}

const toggle = (list: string[], item: string) =>
  list.includes(item) ? list.filter((x) => x !== item) : [...list, item];

function Eyebrow({ children }: { children: React.ReactNode }) {
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
  const [draft, setDraft] = React.useState<AlumniFilters>(value);
  const [companyQuery, setCompanyQuery] = React.useState("");

  // Seed the draft from the applied filters only on open — depending on `value`
  // too would wipe an in-progress edit if the parent re-created it mid-edit.
  React.useEffect(() => {
    if (visible) {
      setDraft(value);
      setCompanyQuery("");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  const visibleCompanies = React.useMemo(
    () => companies.filter((c) => c.toLowerCase().includes(companyQuery.trim().toLowerCase())),
    [companies, companyQuery],
  );

  return (
    <BottomSheet visible={visible} onClose={onClose}>
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

      {skills.length > 0 ? (
        <View className="mt-6 gap-3">
          <Eyebrow>Umiejętności</Eyebrow>
          <View className="flex-row flex-wrap gap-2">
            {skills.map((skill) => (
              <FilterChip
                key={skill}
                label={skill}
                selected={draft.skills.includes(skill)}
                onPress={() => setDraft((d) => ({ ...d, skills: toggle(d.skills, skill) }))}
              />
            ))}
          </View>
        </View>
      ) : null}

      {companies.length > 0 ? (
        <View className="mt-6 gap-3">
          <Eyebrow>Firma</Eyebrow>
          <Input
            value={companyQuery}
            onChangeText={setCompanyQuery}
            placeholder="Szukaj firmy..."
            autoCapitalize="none"
          />
          {visibleCompanies.length > 0 ? (
            <View className="flex-row flex-wrap gap-2">
              {visibleCompanies.map((company) => (
                <FilterChip
                  key={company}
                  label={company}
                  selected={draft.companies.includes(company)}
                  onPress={() =>
                    setDraft((d) => ({ ...d, companies: toggle(d.companies, company) }))
                  }
                />
              ))}
            </View>
          ) : null}
        </View>
      ) : null}

      <View className="mt-6 gap-3">
        <Button size="lg" onPress={() => onApply(draft)}>
          <Text className="font-bold">Zastosuj filtry</Text>
        </Button>
        <Pressable
          onPress={() => {
            setDraft(EMPTY_FILTERS);
            setCompanyQuery("");
          }}
          className="items-center py-1"
        >
          <Text className="text-muted-foreground text-sm font-semibold">Wyczyść wszystko</Text>
        </Pressable>
      </View>
    </BottomSheet>
  );
}
