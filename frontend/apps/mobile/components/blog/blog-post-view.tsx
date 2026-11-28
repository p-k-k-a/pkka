import { ProseContent } from "@/components/content/prose-content";
import { Text } from "@/components/ui/text";
import { AlumniOnlyBadge } from "@/components/events/event-type-badge";
import { ALUMNI_ONLY_POST_LABEL, formatPublishedAtCompact } from "@pkka/domain";
import { PostAudience, type PostResponse } from "@pkka/api";
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
          {post.audience === PostAudience.ALUMNI ? (
            <AlumniOnlyBadge label={ALUMNI_ONLY_POST_LABEL} />
          ) : null}
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

        <ProseContent content={post.content} />
      </View>
    </ScrollView>
  );
}
