import { Card, CardFooter, CardHeader } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import { formatPublishedAt } from "@/lib/utils";
import type { PostSummaryResponse } from "@pkka/api";
import { Link } from "expo-router";
import { Pressable } from "react-native";

type PostCardProps = {
  post: PostSummaryResponse;
};

export function PostCard({ post }: PostCardProps) {
  const { title, slug, publishedAt } = post;

  const card = (
    <Card className="border-0 bg-muted rounded-3xl shadow-md shadow-black/10">
      <CardHeader className="gap-2">
        <Text className="text-xs font-semibold tracking-widest uppercase text-muted-foreground">
          {formatPublishedAt(publishedAt)}
        </Text>
        <Text variant="h3" className="text-xl font-bold leading-tight">
          {title}
        </Text>
      </CardHeader>

      <CardFooter>
        <Text className="text-xs font-bold tracking-widest uppercase text-foreground">
          CZYTAJ WIĘCEJ
        </Text>
      </CardFooter>
    </Card>
  );

  return (
    <Link href={{ pathname: "/blog/[slug]", params: { slug } }} asChild>
      <Pressable className="active:opacity-90">{card}</Pressable>
    </Link>
  );
}
