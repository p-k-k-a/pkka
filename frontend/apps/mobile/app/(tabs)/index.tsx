import { CtaCard } from "@/components/home/cta-card";
import { Hero } from "@/components/home/hero";
import { PostCard } from "@/components/home/post-card";
import { SectionHeading } from "@/components/home/section-heading";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import { useListPosts } from "@pkka/api";
import { keepPreviousData } from "@tanstack/react-query";
import { router } from "expo-router";
import { ChevronDown } from "lucide-react-native";
import * as React from "react";
import { ActivityIndicator, FlatList, Pressable, View } from "react-native";

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
    <View className="gap-10">
      <Hero {...HERO} />
      <View className="gap-10 px-5">
        {!user && <CtaCard {...CTA} onPrimary={() => router.push("/login")} />}
        <SectionHeading title="Publiczne Aktualności" />
      </View>
    </View>
  );
}

function LoadMore({ loading, onPress }: { loading: boolean; onPress: () => void }) {
  const theme = useThemeColors();

  if (loading) return <ActivityIndicator className="py-8" />;

  return (
    <Pressable className="flex-row items-center justify-center gap-2 py-8" onPress={onPress}>
      <Text className="text-accent text-xs font-bold uppercase tracking-widest">
        ZOBACZ STARSZE WPISY
      </Text>
      <ChevronDown size={16} color={theme.accent} />
    </Pressable>
  );
}

export default function HomeScreen() {
  const [pages, setPages] = React.useState(1);
  const { data, isLoading, isError, isFetching } = useListPosts(
    { page: 0, size: PAGE_SIZE * pages },
    { query: { placeholderData: keepPreviousData } },
  );

  const posts = data?.data?.content ?? [];
  const hasMore = data?.data?.last === false;

  return (
    <FlatList
      className="flex-1 bg-background"
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
      ListFooterComponent={
        isLoading ? (
          <ActivityIndicator className="py-8" />
        ) : isError ? (
          <Text className="text-muted-foreground text-sm text-center py-8">
            Nie udało się załadować aktualności.
          </Text>
        ) : hasMore ? (
          <LoadMore loading={isFetching} onPress={() => setPages((count) => count + 1)} />
        ) : null
      }
    />
  );
}
