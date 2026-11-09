import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { formatPublishedAtCompact, placeholderCoverUrl } from "@pkka/domain";
import type { PostSummaryResponse } from "@pkka/api";
import { Image } from "expo-image";
import { Link } from "expo-router";
import { ArrowRight } from "lucide-react-native";
import { Pressable, View } from "react-native";

type PostCardProps = {
  post: PostSummaryResponse;
};

export function PostCard({ post }: PostCardProps) {
  const theme = useThemeColors();
  const { id, title, slug, publishedAt } = post;

  const card = (
    <Card className="bg-muted gap-0 overflow-hidden rounded-3xl border-0 p-0 shadow-none">
      <Image
        source={{ uri: placeholderCoverUrl(id) }}
        contentFit="cover"
        transition={200}
        style={{ width: "100%", aspectRatio: 16 / 9 }}
      />
      <View className="gap-3 px-6 pt-5">
        <Text className="text-muted-foreground text-sm font-semibold">
          {formatPublishedAtCompact(publishedAt)}
        </Text>
        <Text
          role="heading"
          aria-level="3"
          className="text-foreground text-xl font-bold leading-tight"
        >
          {title}
        </Text>
      </View>

      <View className="bg-background mx-6 mb-6 mt-5 flex-row items-center gap-1.5 self-end rounded-lg px-3 py-2">
        <Text className="text-accent text-sm font-medium">Czytaj więcej</Text>
        <ArrowRight size={14} color={theme.accent} />
      </View>
    </Card>
  );

  return (
    <Link href={{ pathname: "/blog/[slug]", params: { slug: slug! } }} asChild>
      <Pressable className="active:opacity-90">{card}</Pressable>
    </Link>
  );
}
