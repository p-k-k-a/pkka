import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { formatPublishedAtCompact } from "@pkka/domain";
import type { PostSummaryResponse } from "@pkka/api";
import { Link } from "expo-router";
import { ArrowRight } from "lucide-react-native";
import { Pressable, View } from "react-native";

type PostCardProps = {
  post: PostSummaryResponse;
};

export function PostCard({ post }: PostCardProps) {
  const theme = useThemeColors();
  const { title, slug, publishedAt } = post;

  const card = (
    <Card className="gap-0 rounded-3xl p-6">
      <View className="gap-3">
        <Text className="text-muted-foreground text-sm font-semibold">
          {formatPublishedAtCompact(publishedAt)}
        </Text>
        <Text className="text-foreground text-xl font-bold leading-tight">{title}</Text>
      </View>

      <View className="border-border mt-6 flex-row items-center justify-end gap-1.5 border-t pt-5">
        <Text className="text-foreground text-xs font-bold uppercase tracking-widest">
          CZYTAJ WIĘCEJ
        </Text>
        <ArrowRight size={14} color={theme.foreground} />
      </View>
    </Card>
  );

  return (
    <Link href={{ pathname: "/blog/[slug]", params: { slug: slug! } }} asChild>
      <Pressable className="active:opacity-90">{card}</Pressable>
    </Link>
  );
}
