import { SkillChips } from "@/components/alumni/skill-chips";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import type { AlumnProfile } from "@/lib/alumni-mock";
import { THEME } from "@/lib/theme";
import { router } from "expo-router";
import { UserRound } from "lucide-react-native";
import { View } from "react-native";

type AlumniCardProps = {
  alumn: AlumnProfile;
};

export function AlumniCard({ alumn }: AlumniCardProps) {
  const name = `${alumn.firstName} ${alumn.lastName}`;
  const hasSubtitle = !!(alumn.currentPosition || alumn.company);

  return (
    <Card className="gap-4 p-5">
      <View className="flex-row gap-4">
        <View className="border-border bg-muted size-16 items-center justify-center overflow-hidden rounded-xl border">
          <UserRound size={32} color={THEME.light.mutedForeground} strokeWidth={1.5} />
        </View>
        <View className="flex-1 gap-1.5">
          <View className="gap-0.5">
            <Text className="text-foreground text-base font-bold leading-tight">{name}</Text>
            {hasSubtitle ? (
              <Text className="text-muted-foreground text-sm">
                {alumn.currentPosition}
                {alumn.currentPosition && alumn.company ? " @ " : ""}
                {alumn.company}
              </Text>
            ) : null}
          </View>
          {alumn.tags.length > 0 ? <SkillChips tags={alumn.tags.slice(0, 3)} /> : null}
        </View>
      </View>

      <Button onPress={() => router.push({ pathname: "/alumni/[id]", params: { id: alumn.id } })}>
        <Text className="font-semibold">Zobacz profil</Text>
      </Button>
    </Card>
  );
}
