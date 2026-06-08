import { CtaCard } from "@/components/home/cta-card";
import { Hero } from "@/components/home/hero";
import { PostCard } from "@/components/home/post-card";
import { SectionHeading } from "@/components/home/section-heading";
import { Text } from "@/components/ui/text";
import { useListPosts } from "@pkka/api";
import { ChevronDown } from "lucide-react-native";
import * as React from "react";
import { ActivityIndicator, FlatList, Pressable, View } from "react-native";

const HERO = {
  title: "Witaj w P.K.K.A",
  body: "Platforma łącząca inżynierów i specjalistów IT Wydziału Informatyki AGH. Współpracuj, rozwijaj się i buduj przyszłość technologii.",
};

const CTA = {
  title: "Dołącz do naszej społeczności",
  subtitle:
    "Dostęp do zamkniętych wydarzeń i katalogu ekspertów. Weryfikacja tylko dla absolwentów WI AGH.",
  primaryLabel: "Zarejestruj się",
  secondaryLabel: "Dowiedz się więcej",
};

function ListHeader() {
  return (
    <View className="px-5 gap-10">
      <Hero {...HERO} />
      <CtaCard {...CTA} />
      <SectionHeading title={"Publiczne\nAktualności"} />
    </View>
  );
}

function ListFooter() {
  return (
    <Pressable
      className="flex-row items-center justify-center gap-2 py-8"
      onPress={() => {
        console.log("nothing yet");
      }}
    >
      <Text className="text-xs font-bold tracking-widest uppercase text-foreground">
        ZOBACZ STARSZE WPISY
      </Text>
      <ChevronDown size={16} />
    </Pressable>
  );
}

export default function HomeScreen() {
  const { data, isLoading, isError } = useListPosts({ size: 10 });

  const posts = data?.data?.content ?? [];

  return (
    <FlatList
      className="flex-1 bg-background"
      data={posts}
      keyExtractor={(item) => item.id ?? item.slug ?? ""}
      renderItem={({ item }) => (
        <View className="px-5">
          <PostCard post={item} />
        </View>
      )}
      ItemSeparatorComponent={() => <View className="h-5" />}
      ListHeaderComponent={ListHeader}
      ListHeaderComponentStyle={{ marginBottom: 20 }}
      ListFooterComponent={
        isLoading ? (
          <ActivityIndicator className="py-8" />
        ) : isError ? (
          <Text className="text-muted-foreground text-sm text-center py-8">
            Nie udało się załadować aktualności.
          </Text>
        ) : (
          <ListFooter />
        )
      }
    />
  );
}
