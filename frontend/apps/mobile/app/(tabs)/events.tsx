import { useIsAlumni } from "@/components/auth/require-alumni";
import { EventCard } from "@/components/events/event-card";
import {
  countActiveEventFilters,
  EMPTY_EVENT_FILTERS,
  EventFilterSheet,
  type EventFilters,
} from "@/components/events/event-filter-sheet";
import { CtaCard } from "@/components/home/cta-card";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { END_REACHED_THRESHOLD, nextPageParam, pageItems } from "@/lib/pagination";
import { useThemeColors } from "@/lib/theme";
import { Audience, EventTimeframe, useListEventsInfinite } from "@pkka/api";
import { router } from "expo-router";
import { SlidersHorizontal } from "lucide-react-native";
import { useMemo, useState } from "react";
import { ActivityIndicator, FlatList, View } from "react-native";

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

function FilterButton({ activeCount, onPress }: { activeCount: number; onPress: () => void }) {
  const theme = useThemeColors();

  return (
    <Button variant="outline" className="active:bg-muted" onPress={onPress}>
      <SlidersHorizontal size={16} color={theme.foreground} />
      <Text className="group-active:text-foreground text-sm font-semibold uppercase tracking-wider">
        Filtruj
      </Text>
      {activeCount > 0 ? (
        <View className="bg-primary ml-1 size-5 items-center justify-center rounded-full">
          <Text className="text-primary-foreground text-[11px] font-bold">{activeCount}</Text>
        </View>
      ) : null}
    </Button>
  );
}

function ListHeader({
  activeFilterCount,
  onOpenFilters,
}: {
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
      <View className="px-5">
        <FilterButton activeCount={activeFilterCount} onPress={onOpenFilters} />
      </View>
    </View>
  );
}

export default function EventsScreen() {
  const [filters, setFilters] = useState<EventFilters>(EMPTY_EVENT_FILTERS);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const isAlumni = useIsAlumni();
  const { timeframe } = filters;
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
        showAudience={isAlumni}
        onClose={() => setFiltersOpen(false)}
        onApply={(next) => {
          setFilters(next);
          setFiltersOpen(false);
        }}
      />
    </>
  );
}
