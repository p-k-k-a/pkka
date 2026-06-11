import { BlogPostView } from "@/components/blog/blog-post-view";
import { DetailHeader } from "@/components/blog/detail-header";
import { Text } from "@/components/ui/text";
import { useGetPost } from "@pkka/api";
import { router, Stack, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, View } from "react-native";

export default function BlogPostScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const { data, isError } = useGetPost(slug ?? "", {
    query: { enabled: !!slug },
  });

  const post = data?.data;

  return (
    <View className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: false }} />
      <DetailHeader title="Szczegóły wpisu" onBack={() => router.back()} />

      {isError ? (
        <Text className="py-12 text-center text-sm text-muted-foreground">
          Nie udało się załadować wpisu.
        </Text>
      ) : !post ? (
        <ActivityIndicator className="py-12" />
      ) : (
        <BlogPostView post={post} />
      )}
    </View>
  );
}
