import { useIsAlumni } from "@/components/auth/require-alumni";
import { EventCard } from "@/components/events/event-card";
import {
  countActiveEventFilters,
  EMPTY_EVENT_FILTERS,
  EventFilterSheet,
  type EventFilters,
} from "@/components/events/event-filter-sheet";
import { CtaCard } from "@/components/home/cta-card";
import { PageHeader } from "@/components/ui/page-header";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { END_REACHED_THRESHOLD, nextPageParam, pageItems } from "@/lib/pagination";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { useThemeColors } from "@/lib/theme";
import { cn } from "@/lib/utils";
import { Audience, EventTimeframe, useListEventsInfinite } from "@pkka/api";
import { router } from "expo-router";
import { SlidersHorizontal } from "lucide-react-native";
import { useMemo, useState } from "react";
import { ActivityIndicator, FlatList, Pressable, View } from "react-native";

const PAGE_SIZE = 20;

const CTA = {
  title: "Chcesz brać udział w wydarzeniach zamkniętych?",
  subtitle:
    "Zaloguj się jako absolwent lub student Wydziału Informatyki AGH, aby uzyskać dostęp do pełnego kalendarza i mentoringu.",
  primaryLabel: "Zaloguj się do portalu",
};

const DESCRIPTION =
  "Przeglądaj publiczne spotkania i prelekcje organizowane przez społeczność Alumni WI AGH. Dołącz do nas i buduj sieć kontaktów.";
const ALUMNI_DESCRIPTION =
  "Przeglądaj spotkania i prelekcje społeczności Alumni WI AGH, także te tylko dla absolwentów.";

const TIMEFRAMES = [
  { value: EventTimeframe.UPCOMING, label: "Nadchodzące" },
  { value: EventTimeframe.PAST, label: "Archiwum" },
];

function TimeframeTabs({
  value,
  onChange,
}: {
  value: EventTimeframe;
  onChange: (timeframe: EventTimeframe) => void;
}) {
  return (
    <ToggleGroup
      type="single"
      variant="outline"
      value={value}
      onValueChange={(next) => {
        if (next) onChange(next as EventTimeframe);
      }}
    >
      {TIMEFRAMES.map((option, index) => (
        <ToggleGroupItem
          key={option.value}
          value={option.value}
          isFirst={index === 0}
          isLast={index === TIMEFRAMES.length - 1}
          className={cn(
            "h-10 flex-1",
            value === option.value
              ? "bg-primary/15 border-primary active:bg-primary/25"
              : "active:bg-muted",
          )}
        >
          <Text className="text-foreground text-[13px] font-bold">{option.label}</Text>
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}

function FilterButton({ activeCount, onPress }: { activeCount: number; onPress: () => void }) {
  const theme = useThemeColors();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel="Filtry"
      className={cn(
        "size-10 items-center justify-center rounded-md border",
        activeCount > 0 ? "border-accent bg-accent/10" : "border-border",
      )}
    >
      <SlidersHorizontal size={18} color={activeCount > 0 ? theme.accent : theme.foreground} />
    </Pressable>
  );
}

function ListHeader({
  timeframe,
  onTimeframeChange,
  activeFilterCount,
  onOpenFilters,
}: {
  timeframe: EventTimeframe;
  onTimeframeChange: (timeframe: EventTimeframe) => void;
  activeFilterCount: number;
  onOpenFilters: () => void;
}) {
  const { user } = useAuth();
  const isAlumni = useIsAlumni();

  return (
    <View className="gap-6">
      <PageHeader title="Wydarzenia" description={isAlumni ? ALUMNI_DESCRIPTION : DESCRIPTION} />
      {!user && (
        <View className="px-5">
          <CtaCard {...CTA} onPrimary={() => router.push("/login")} />
        </View>
      )}
      <View className="gap-6 px-5">
        <View className="flex-row items-center gap-3">
          <View className="flex-1">
            <TimeframeTabs value={timeframe} onChange={onTimeframeChange} />
          </View>
          {isAlumni ? (
            <FilterButton activeCount={activeFilterCount} onPress={onOpenFilters} />
          ) : null}
        </View>
      </View>
    </View>
  );
}

export default function EventsScreen() {
  const [timeframe, setTimeframe] = useState<EventTimeframe>(EventTimeframe.UPCOMING);
  const [filters, setFilters] = useState<EventFilters>(EMPTY_EVENT_FILTERS);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const isAlumni = useIsAlumni();
  const archive = timeframe === EventTimeframe.PAST;
  const { data, isPending, isError, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useListEventsInfinite(
      {
        size: PAGE_SIZE,
        timeframe,
        ...(isAlumni && filters.alumniOnly ? { audience: Audience.ALL_ALUMNI } : {}),
        ...(archive ? { sort: ["startsAt,desc"] } : {}),
      },
      { query: { initialPageParam: 0, getNextPageParam: nextPageParam } },
    );

  const events = useMemo(() => pageItems(data?.pages), [data]);

  return (
    <>
      <FlatList
        className="flex-1 bg-background"
        contentContainerClassName="pb-8"
        showsVerticalScrollIndicator={false}
        data={events}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View className="px-5">
            <EventCard event={item} />
          </View>
        )}
        ItemSeparatorComponent={() => <View className="h-6" />}
        ListHeaderComponent={
          <ListHeader
            timeframe={timeframe}
            onTimeframeChange={setTimeframe}
            activeFilterCount={countActiveEventFilters(filters)}
            onOpenFilters={() => setFiltersOpen(true)}
          />
        }
        ListHeaderComponentStyle={{ marginBottom: 24 }}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
        }}
        onEndReachedThreshold={END_REACHED_THRESHOLD}
        ListFooterComponent={
          isPending || isFetchingNextPage ? (
            <ActivityIndicator className="py-8" />
          ) : isError ? (
            <Text className="py-8 text-center text-sm text-muted-foreground">
              Nie udało się załadować wydarzeń.
            </Text>
          ) : events.length === 0 ? (
            <Text className="py-8 text-center text-sm text-muted-foreground">
              {archive ? "Brak wydarzeń w archiwum." : "Brak nadchodzących wydarzeń."}
            </Text>
          ) : null
        }
      />
      <EventFilterSheet
        visible={filtersOpen}
        value={filters}
        onClose={() => setFiltersOpen(false)}
        onApply={(next) => {
          setFilters(next);
          setFiltersOpen(false);
        }}
      />
    </>
  );
}
