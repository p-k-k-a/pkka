import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { EventTimeframe } from "@pkka/api";
import { useEffect, useState } from "react";
import { Pressable, View } from "react-native";

export type EventFilters = {
  timeframe: EventTimeframe;
  alumniOnly: boolean;
};

export const EMPTY_EVENT_FILTERS: EventFilters = {
  timeframe: EventTimeframe.UPCOMING,
  alumniOnly: false,
};

export function countActiveEventFilters(filters: EventFilters) {
  return (
    Number(filters.timeframe !== EMPTY_EVENT_FILTERS.timeframe) +
    Number(filters.alumniOnly !== EMPTY_EVENT_FILTERS.alumniOnly)
  );
}

const TIMEFRAME_OPTIONS = [
  {
    value: EventTimeframe.UPCOMING,
    label: "Nadchodzące",
    description: "Wydarzenia, na które możesz się zapisać",
  },
  {
    value: EventTimeframe.PAST,
    label: "Archiwum",
    description: "Wydarzenia, które już się odbyły",
  },
];

const AUDIENCE_OPTIONS = [
  {
    value: false,
    label: "Wszystkie wydarzenia",
    description: "Publiczne i tylko dla alumnów",
  },
  {
    value: true,
    label: "Tylko dla alumnów",
    description: "Spotkania zamknięte dla absolwentów WI AGH",
  },
];

type Option<T> = { value: T; label: string; description: string };

function RadioSection<T>({
  title,
  options,
  value,
  onChange,
}: {
  title: string;
  options: Option<T>[];
  value: T;
  onChange: (next: T) => void;
}) {
  return (
    <View className="gap-3">
      <Text className="text-muted-foreground text-[10px] font-semibold uppercase tracking-widest">
        {title}
      </Text>
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.label}
            role="radio"
            accessibilityState={{ checked: selected }}
            onPress={() => onChange(option.value)}
            className={cn(
              "flex-row items-center gap-3 rounded-lg border px-4 py-3",
              selected ? "border-accent bg-accent/10" : "border-border",
            )}
          >
            <View
              className={cn(
                "size-5 items-center justify-center rounded-full border-2",
                selected ? "border-accent" : "border-muted-foreground/40",
              )}
            >
              {selected ? <View className="bg-accent size-2.5 rounded-full" /> : null}
            </View>
            <View className="flex-1 gap-0.5">
              <Text className="text-foreground text-base font-semibold">{option.label}</Text>
              <Text className="text-muted-foreground text-sm">{option.description}</Text>
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}

type EventFilterSheetProps = {
  visible: boolean;
  value: EventFilters;
  showAudience: boolean;
  onClose: () => void;
  onApply: (filters: EventFilters) => void;
};

export function EventFilterSheet({
  visible,
  value,
  showAudience,
  onClose,
  onApply,
}: EventFilterSheetProps) {
  const [draft, setDraft] = useState(value);

  useEffect(() => {
    if (visible) setDraft(value);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  return (
    <BottomSheet visible={visible} onClose={onClose}>
      <Text variant="h3" className="text-2xl font-bold">
        Filtry
      </Text>

      <View className="mt-6 gap-6">
        <RadioSection
          title="Kiedy"
          options={TIMEFRAME_OPTIONS}
          value={draft.timeframe}
          onChange={(timeframe) => setDraft((d) => ({ ...d, timeframe }))}
        />
        {showAudience ? (
          <RadioSection
            title="Dla kogo"
            options={AUDIENCE_OPTIONS}
            value={draft.alumniOnly}
            onChange={(alumniOnly) => setDraft((d) => ({ ...d, alumniOnly }))}
          />
        ) : null}
      </View>

      <Button size="lg" className="mt-6" onPress={() => onApply(draft)}>
        <Text className="font-bold">Pokaż wydarzenia</Text>
      </Button>
    </BottomSheet>
  );
}
