import { useSeesAllMaterials } from "@/components/auth/require-alumni";
import { MaterialCard } from "@/components/materials/material-card";
import { MaterialTypeFilter } from "@/components/materials/material-type-filter";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { useMaterials } from "@/lib/materials";
import { END_REACHED_THRESHOLD } from "@/lib/pagination";
import { useThemeColors } from "@/lib/theme";
import type { MaterialType } from "@pkka/api";
import { router } from "expo-router";
import { Calendar, Search, X } from "lucide-react-native";
import { useState } from "react";
import { ActivityIndicator, FlatList, Pressable, RefreshControl, View } from "react-native";

type EventFilter = { id: string; title?: string };

function EventFilterChip({ title, onClear }: { title?: string; onClear: () => void }) {
  const theme = useThemeColors();

  return (
    <View className="bg-muted flex-row items-center gap-2 self-start rounded-lg py-1 pl-3 pr-1">
      <Calendar size={16} color={theme.mutedForeground} />
      <Text numberOfLines={1} className="text-foreground shrink text-sm">
        {title ?? "Wybrane wydarzenie"}
      </Text>
      <Pressable
        role="button"
        accessibilityLabel="Usuń filtr wydarzenia"
        hitSlop={8}
        onPress={onClear}
        className="active:bg-background size-8 items-center justify-center rounded-md"
      >
        <X size={16} color={theme.foreground} />
      </Pressable>
    </View>
  );
}

function VerificationHint() {
  return (
    <View className="border-border mx-5 mt-8 gap-3 border-t pt-6">
      <Text className="text-muted-foreground text-sm leading-relaxed">
        Część materiałów jest dostępna tylko dla zweryfikowanych alumnów. Zweryfikuj się, żeby
        zobaczyć wszystkie.
      </Text>
      <Button
        variant="outline"
        className="active:bg-muted self-start"
        onPress={() => router.push("/application")}
      >
        <Text className="group-active:text-foreground text-sm font-semibold uppercase tracking-wider">
          Zweryfikuj się
        </Text>
      </Button>
    </View>
  );
}

export function MaterialsLibrary({ initialEvent }: { initialEvent?: EventFilter }) {
  const theme = useThemeColors();
  const [query, setQuery] = useState("");
  const [type, setType] = useState<MaterialType | null>(null);
  const [event, setEvent] = useState<EventFilter | null>(initialEvent ?? null);
  const [refreshing, setRefreshing] = useState(false);
  const { isLoading: isRoleLoading, seesAll } = useSeesAllMaterials();

  const {
    materials,
    total,
    isLoading,
    isError,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useMaterials({ query, type, eventId: event?.id ?? null });

  const filtered = query.trim() !== "" || type !== null || event !== null;
  const eventTitle =
    event?.title ?? materials.find((material) => material.eventId === event?.id)?.eventTitle;

  async function onRefresh() {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  }

  function clearFilters() {
    setQuery("");
    setType(null);
    setEvent(null);
  }

  return (
    <View className="bg-background flex-1">
      <View className="border-border gap-4 border-b px-5 py-5">
        <View className="justify-center">
          <View className="pointer-events-none absolute bottom-0 left-3 top-0 z-10 justify-center">
            <Search size={18} color={theme.mutedForeground} />
          </View>
          <Input
            value={query}
            onChangeText={setQuery}
            placeholder="Szukaj materiałów"
            accessibilityLabel="Szukaj materiałów"
            autoCapitalize="none"
            autoCorrect={false}
            returnKeyType="search"
            className="bg-muted focus:border-primary dark:focus:border-primary border-transparent pl-10 dark:border-transparent"
          />
        </View>

        <MaterialTypeFilter value={type} onChange={setType} />

        {event ? <EventFilterChip title={eventTitle} onClear={() => setEvent(null)} /> : null}
      </View>

      <FlatList
        className="flex-1"
        showsVerticalScrollIndicator={false}
        data={materials}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View className="px-5">
            <MaterialCard material={item} />
          </View>
        )}
        ItemSeparatorComponent={() => <View className="h-4" />}
        contentContainerStyle={{ paddingBottom: 48, paddingTop: 20 }}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
        }}
        onEndReachedThreshold={END_REACHED_THRESHOLD}
        ListHeaderComponent={
          materials.length > 0 ? (
            <Text className="text-muted-foreground px-5 pb-4 text-sm">
              Znaleziono: <Text className="text-foreground text-sm font-semibold">{total}</Text>
            </Text>
          ) : null
        }
        ListFooterComponent={
          <>
            {isFetchingNextPage ? <ActivityIndicator className="py-6" /> : null}
            {!isRoleLoading && !seesAll && !isLoading && !hasNextPage ? <VerificationHint /> : null}
          </>
        }
        ListEmptyComponent={
          <View className="items-center gap-4 px-5 py-16">
            {isLoading ? (
              <ActivityIndicator />
            ) : isError ? (
              <>
                <Text className="text-muted-foreground text-center text-sm">
                  Nie udało się załadować materiałów.
                </Text>
                <Button variant="outline" onPress={() => void refetch()}>
                  <Text className="text-sm font-semibold">Spróbuj ponownie</Text>
                </Button>
              </>
            ) : (
              <>
                <Text className="text-muted-foreground text-center text-sm">
                  {filtered
                    ? "Brak materiałów spełniających kryteria."
                    : "Nie ma jeszcze żadnych materiałów."}
                </Text>
                {filtered ? (
                  <Button variant="outline" onPress={clearFilters}>
                    <Text className="text-sm font-semibold">Wyczyść filtry</Text>
                  </Button>
                ) : null}
              </>
            )}
          </View>
        }
      />
    </View>
  );
}
