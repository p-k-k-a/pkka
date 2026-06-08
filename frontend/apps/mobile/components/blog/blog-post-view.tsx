import { Text } from "@/components/ui/text";
import type { PostResponse } from "@pkka/api";
import { useTheme } from "@react-navigation/native";
import { ImageIcon } from "lucide-react-native";
import { ScrollView, View } from "react-native";

type BlogPostViewProps = {
  post: PostResponse;
};

function BlogPostView({ post }: BlogPostViewProps) {
  const { colors } = useTheme();

  const publishedAt = post.publishedAt ? new Date(post.publishedAt) : null;
  const publishedLabel =
    publishedAt && !Number.isNaN(publishedAt.getTime())
      ? `${publishedAt.toLocaleDateString("pl-PL")} - ${publishedAt.toLocaleTimeString("pl-PL", {
          hour: "2-digit",
          minute: "2-digit",
        })}`
      : "Nieznana data";

  return (
    <ScrollView
      className="flex-1"
      contentContainerStyle={{ paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
    >
      <View className="gap-6 px-5 pt-5">
        <View className="aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-xl border border-border bg-muted">
          <ImageIcon size={56} color={colors.border} />
        </View>

        <View className="gap-2">
          <Text className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            {publishedLabel}
          </Text>
          <Text variant="h1" className="text-left text-3xl leading-tight">
            {post.title}
          </Text>
        </View>

        <View className="h-px bg-border" />

        <View className="gap-3">
          {post.excerpt ? (
            <>
              <Text className="text-xs font-bold uppercase tracking-widest text-foreground">
                O wpisie
              </Text>
              <Text className="leading-7 text-muted-foreground">{post.excerpt}</Text>
            </>
          ) : null}
          {post.content ? (
            <Text className="leading-7 text-muted-foreground">{post.content}</Text>
          ) : null}
        </View>
      </View>
    </ScrollView>
  );
}

export { BlogPostView };
