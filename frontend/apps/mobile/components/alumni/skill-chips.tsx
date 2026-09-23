import { Badge } from "@/components/ui/badge";
import { Text } from "@/components/ui/text";
import type { UserTagResponse } from "@pkka/api";
import { View } from "react-native";

type SkillChipsProps = {
  tags: UserTagResponse[];
};

export function SkillChips({ tags }: SkillChipsProps) {
  return (
    <View className="flex-row flex-wrap gap-2">
      {tags.map((tag) => (
        <Badge
          key={tag.id}
          variant="secondary"
          className="border-accent/25 bg-accent/10 rounded-lg px-3 py-1"
        >
          <Text className="text-foreground text-[11px] font-semibold tracking-wider">
            {tag.name.toUpperCase()}
          </Text>
        </Badge>
      ))}
    </View>
  );
}
