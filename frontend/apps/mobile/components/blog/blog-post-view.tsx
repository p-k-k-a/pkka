import { Text } from "@/components/ui/text";
import { formatPublishedAt } from "@/lib/utils";
import type { PostResponse } from "@pkka/api";
import { ScrollView, View } from "react-native";

type BlogPostViewProps = {
  post: PostResponse;
};

export function BlogPostView({ post }: BlogPostViewProps) {
  return (
    <ScrollView
      className="flex-1"
      contentContainerStyle={{ paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
    >
      <View className="gap-6 px-5 pt-5">
        <View className="gap-2">
          <Text className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            {formatPublishedAt(post.publishedAt)}
          </Text>
          <Text variant="h1" className="text-left text-3xl leading-tight">
            {post.title}
          </Text>
        </View>

        <View className="h-px bg-border" />

        <View className="gap-3">
          <Text className="text-xs font-bold uppercase tracking-widest text-foreground">
            O wpisie
          </Text>
          <Text className="leading-7 text-muted-foreground">{post.content}</Text>
        </View>
      </View>
    </ScrollView>
  );
}
