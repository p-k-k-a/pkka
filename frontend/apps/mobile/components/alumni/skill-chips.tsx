import { Badge } from "@/components/ui/badge";
import { Text } from "@/components/ui/text";
import type { AlumnTag } from "@/lib/alumni-mock";
import { View } from "react-native";

type SkillChipsProps = {
  tags: AlumnTag[];
};

export function SkillChips({ tags }: SkillChipsProps) {
  return (
    <View className="flex-row flex-wrap gap-2">
      {tags.map((tag) => (
        <Badge key={tag.id} variant="secondary" className="rounded-md bg-muted px-3 py-1">
          <Text className="text-xs font-semibold text-foreground">{tag.name.toUpperCase()}</Text>
        </Badge>
      ))}
    </View>
  );
}
