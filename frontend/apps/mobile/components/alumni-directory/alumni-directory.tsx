import { AlumniCard } from "@/components/alumni-directory/alumni-card";
import { FilterSheet } from "@/components/alumni-directory/filter-sheet";
import { SortSheet } from "@/components/alumni-directory/sort-sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import {
  countActiveFilters,
  DEFAULT_SORT,
  EMPTY_FILTERS,
  filterAlumni,
  sortAlumni,
  uniqueCompanies,
  uniqueSkills,
  useAlumniDirectory,
  type AlumniFilters,
  type SortOption,
} from "@/lib/alumni-directory";
import { THEME } from "@/lib/theme";
import { ArrowUpDown, Search, SlidersHorizontal } from "lucide-react-native";
import { useCallback, useMemo, useState } from "react";
import { ActivityIndicator, FlatList, RefreshControl, View } from "react-native";

type ActiveSheet = "filter" | "sort" | null;

export function AlumniDirectory() {
  const { alumni, isLoading, isError, refetch } = useAlumniDirectory();
  const [query, setQuery] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [filters, setFilters] = useState<AlumniFilters>(EMPTY_FILTERS);
  const [sort, setSort] = useState<SortOption>(DEFAULT_SORT);
  const [sheet, setSheet] = useState<ActiveSheet>(null);

  const skills = useMemo(() => uniqueSkills(alumni), [alumni]);
  const companies = useMemo(() => uniqueCompanies(alumni), [alumni]);

  const results = useMemo(
    () => sortAlumni(filterAlumni(alumni, query, filters), sort),
    [alumni, query, filters, sort],
  );

  const activeCount = countActiveFilters(filters);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  }, [refetch]);

  return (
    <View className="bg-background flex-1">
      {/* Pinned above the list: keeping the search TextInput out of the FlatList
          header avoids Android focus loss when results change on each keystroke. */}
      <View className="gap-4 px-5 pb-4 pt-8">
        <Text className="text-foreground font-heading text-2xl font-bold uppercase tracking-tight">
          Katalog Alumnów
        </Text>

        <View className="justify-center">
          <View className="absolute bottom-0 left-3 top-0 z-10 justify-center">
            <Search size={18} color={THEME.light.mutedForeground} />
          </View>
          <Input
            value={query}
            onChangeText={setQuery}
            placeholder="Szukaj po nazwisku, firmie..."
            autoCapitalize="none"
            className="pl-10"
          />
        </View>

        <View className="flex-row gap-3">
          <Button
            variant="outline"
            className="flex-1 active:bg-muted"
            onPress={() => setSheet("filter")}
          >
            <SlidersHorizontal size={16} color={THEME.light.foreground} />
            <Text className="group-active:text-foreground text-sm font-semibold uppercase tracking-wider">
              Filtruj
            </Text>
            {activeCount > 0 ? (
              <View className="bg-primary ml-1 size-5 items-center justify-center rounded-full">
                <Text className="text-primary-foreground text-[11px] font-bold">{activeCount}</Text>
              </View>
            ) : null}
          </Button>
          <Button
            variant="outline"
            className="flex-1 active:bg-muted"
            onPress={() => setSheet("sort")}
          >
            <ArrowUpDown size={16} color={THEME.light.foreground} />
            <Text className="group-active:text-foreground text-sm font-semibold uppercase tracking-wider">
              Sortuj
            </Text>
          </Button>
        </View>
      </View>

      <FlatList
        className="flex-1"
        data={results}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View className="px-5">
            <AlumniCard alumn={item} />
          </View>
        )}
        ItemSeparatorComponent={() => <View className="h-4" />}
        contentContainerStyle={{ paddingBottom: 48, paddingTop: 4 }}
        keyboardShouldPersistTaps="handled"
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        ListEmptyComponent={
          <View className="px-5 py-16">
            {isLoading ? (
              <ActivityIndicator />
            ) : (
              <Text className="text-muted-foreground text-center text-sm">
                {isError
                  ? "Nie udało się załadować alumnów."
                  : "Brak alumnów spełniających kryteria."}
              </Text>
            )}
          </View>
        }
      />

      <FilterSheet
        visible={sheet === "filter"}
        value={filters}
        skills={skills}
        companies={companies}
        onClose={() => setSheet(null)}
        onApply={(next) => {
          setFilters(next);
          setSheet(null);
        }}
      />
      <SortSheet
        visible={sheet === "sort"}
        value={sort}
        onClose={() => setSheet(null)}
        onApply={(next) => {
          setSort(next);
          setSheet(null);
        }}
      />
    </View>
  );
}
