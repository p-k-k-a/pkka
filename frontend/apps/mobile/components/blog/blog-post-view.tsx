import { Avatar, AvatarImage } from "@/components/ui/avatar";
import { Text } from "@/components/ui/text";
import type { BlogPost } from "@pkka/api";
import { useTheme } from "@react-navigation/native";
import { Image } from "expo-image";
import { ImageIcon } from "lucide-react-native";
import { ScrollView, View } from "react-native";

type BlogPostViewProps = {
  post: BlogPost;
};

function BlogPostView({ post }: BlogPostViewProps) {
  const { colors } = useTheme();
  const hasImage = post.imageUrl.startsWith("http");

  const publishedAt = new Date(post.publishedAt);
  const publishedLabel = `${publishedAt.toLocaleDateString("pl-PL")} - ${publishedAt.toLocaleTimeString(
    "pl-PL",
    { hour: "2-digit", minute: "2-digit" },
  )}`;

  return (
    <ScrollView
      className="flex-1"
      contentContainerStyle={{ paddingBottom: 32 }}
      showsVerticalScrollIndicator={false}
    >
      <View className="gap-6 px-5 pt-5">
        <View className="aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-xl border border-border bg-muted">
          {hasImage ? (
            <Image
              source={{ uri: post.imageUrl }}
              contentFit="cover"
              style={{ width: "100%", height: "100%" }}
            />
          ) : (
            <ImageIcon size={56} color={colors.border} />
          )}
        </View>

        <View className="gap-2">
          <Text className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            {publishedLabel}
          </Text>
          <Text variant="h1" className="text-left text-3xl leading-tight">
            {post.title}
          </Text>
        </View>

        <View className="flex-row items-center gap-3">
          <Avatar alt={post.author.name} className="size-9">
            <AvatarImage source={{ uri: post.author.avatarUrl }} />
          </Avatar>
          <View className="flex-1">
            <Text className="text-sm font-semibold text-foreground">{post.author.name}</Text>
            <Text className="text-xs uppercase tracking-widest text-muted-foreground">
              {post.author.role}
            </Text>
          </View>
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

export { BlogPostView };
