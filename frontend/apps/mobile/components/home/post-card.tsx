import { Avatar, AvatarImage } from "@/components/ui/avatar";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import type { Announcement } from "@pkka/api";
import * as React from "react";
import { Pressable, View } from "react-native";

type PostCardProps = {
  announcement: Announcement;
  onPress?: () => void;
};

function PostCard({ announcement, onPress }: PostCardProps) {
  const { title, date, time, author } = announcement;

  return (
    <Card className="border-0 bg-card shadow-md shadow-black/10">
      <CardHeader className="gap-2">
        <Text className="text-xs font-semibold tracking-widest uppercase text-muted-foreground">
          {date} - {time}
        </Text>
        <Text variant="h3" className="text-xl font-bold leading-tight">
          {title}
        </Text>
      </CardHeader>

      <CardContent className="pb-2">
        <View className="flex-row items-center gap-3">
          <Avatar alt={author.name} className="size-9">
            <AvatarImage source={{ uri: author.avatarUrl }} />
          </Avatar>
          <View className="flex-1">
            <Text className="text-sm font-semibold text-foreground">{author.name}</Text>
            <Text className="text-xs uppercase tracking-widest text-muted-foreground">
              {author.role}
            </Text>
          </View>
        </View>
      </CardContent>

      <CardFooter>
        <Pressable onPress={onPress}>
          <Text className="text-xs font-bold tracking-widest uppercase text-foreground">
            CZYTAJ WIĘCEJ
          </Text>
        </Pressable>
      </CardFooter>
    </Card>
  );
}

export { PostCard };
