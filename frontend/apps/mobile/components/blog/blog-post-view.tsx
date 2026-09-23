import { Text } from "@/components/ui/text";
import { formatPublishedAtCompact } from "@pkka/domain";
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
          <Text className="text-muted-foreground text-sm font-semibold">
            {formatPublishedAtCompact(post.publishedAt)}
          </Text>
          <Text
            role="heading"
            aria-level="1"
            className="font-heading text-foreground text-[28px] font-semibold leading-tight tracking-tight"
          >
            {post.title}
          </Text>
        </View>

        <View className="h-px bg-border" />

        <View className="gap-3">
          <Text className="font-heading text-foreground text-xl font-semibold">O wpisie</Text>
          <Text className="text-foreground/90 text-base leading-7">{post.content}</Text>
        </View>
      </View>
    </ScrollView>
  );
}
