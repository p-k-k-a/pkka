import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import type { PostSummaryResponse } from "@pkka/api";
import { type Href, Link } from "expo-router";
import { Pressable } from "react-native";

type PostCardProps = {
  post: PostSummaryResponse;
};

function formatPublishedAt(publishedAt?: string) {
  if (!publishedAt) return "Nieznana data";

  const date = new Date(publishedAt);
  if (Number.isNaN(date.getTime())) return "Nieznana data";

  const dateLabel = date.toLocaleDateString("pl-PL");
  const timeLabel = date.toLocaleTimeString("pl-PL", { hour: "2-digit", minute: "2-digit" });
  return `${dateLabel} - ${timeLabel}`;
}

function PostCard({ post }: PostCardProps) {
  const slug = post.slug ?? post.id ?? "";

  return (
    <Link href={`/blog/${slug}` as Href} asChild>
      <Pressable className="active:opacity-90">
        <Card className="border-0 bg-secondary rounded-3xl shadow-md shadow-black/10">
          <CardHeader className="gap-2">
            <Text className="text-xs font-semibold tracking-widest uppercase text-muted-foreground">
              {formatPublishedAt(post.publishedAt)}
            </Text>
            <Text variant="h3" className="text-xl font-bold leading-tight">
              {post.title}
            </Text>
          </CardHeader>

          {post.excerpt ? (
            <CardContent className="pb-2">
              <Text className="text-sm leading-relaxed text-muted-foreground">{post.excerpt}</Text>
            </CardContent>
          ) : null}

          <CardFooter>
            <Text className="text-xs font-bold tracking-widest uppercase text-foreground">
              CZYTAJ WIĘCEJ
            </Text>
          </CardFooter>
        </Card>
      </Pressable>
    </Link>
  );
}

export { PostCard };
