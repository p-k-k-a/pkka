import { PageHeader } from "@/components/ui/page-header";
import { CtaCard } from "@/components/home/cta-card";
import { PostCard } from "@/components/home/post-card";
import { SectionHeading } from "@/components/home/section-heading";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { END_REACHED_THRESHOLD, nextPageParam, pageItems } from "@/lib/pagination";
import { useListPostsInfinite } from "@pkka/api";
import { router } from "expo-router";
import * as React from "react";
import { ActivityIndicator, FlatList, View } from "react-native";

const PAGE_SIZE = 20;

const HERO = {
  title: "Witaj w P.K.K.A",
  body: "Platforma łącząca inżynierów i specjalistów IT Wydziału Informatyki AGH. Współpracuj, rozwijaj się i buduj przyszłość technologii.",
};

const CTA = {
  title: "Dołącz do naszej społeczności",
  subtitle:
    "Dostęp do zamkniętych wydarzeń i katalogu ekspertów. Weryfikacja tylko dla absolwentów WI AGH.",
  primaryLabel: "Dołącz",
  secondaryLabel: "Dowiedz się więcej",
};

function ListHeader() {
  const { user } = useAuth();

  return (
    <View className="gap-6">
      <PageHeader title={HERO.title} description={HERO.body} />
      <View className="gap-6 px-5">
        {!user && <CtaCard {...CTA} onPrimary={() => router.push("/login")} />}
        <SectionHeading title="Publiczne Aktualności" />
      </View>
    </View>
  );
}

export default function HomeScreen() {
  const { data, isPending, isError, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useListPostsInfinite(
      { size: PAGE_SIZE },
      { query: { initialPageParam: 0, getNextPageParam: nextPageParam } },
    );

  const posts = React.useMemo(() => pageItems(data?.pages), [data]);

  return (
    <FlatList
      className="flex-1 bg-background"
      contentContainerClassName="pb-8"
      showsVerticalScrollIndicator={false}
      data={posts}
      keyExtractor={(item) => item.id}
      renderItem={({ item }) => (
        <View className="px-5">
          <PostCard post={item} />
        </View>
      )}
      ItemSeparatorComponent={() => <View className="h-6" />}
      ListHeaderComponent={ListHeader}
      ListHeaderComponentStyle={{ marginBottom: 24 }}
      onEndReached={() => {
        if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
      }}
      onEndReachedThreshold={END_REACHED_THRESHOLD}
      ListFooterComponent={
        isPending || isFetchingNextPage ? (
          <ActivityIndicator className="py-8" />
        ) : isError ? (
          <Text className="text-muted-foreground text-sm text-center py-8">
            Nie udało się załadować aktualności.
          </Text>
        ) : null
      }
    />
  );
}
