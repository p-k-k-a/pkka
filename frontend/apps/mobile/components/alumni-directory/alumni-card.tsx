import { SkillChips } from "@/components/alumni/skill-chips";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import type { AlumniListItemResponse } from "@pkka/api";
import { router } from "expo-router";
import { ArrowRight, UserRound } from "lucide-react-native";
import { View } from "react-native";

type AlumniCardProps = {
  alumn: AlumniListItemResponse;
};

export function AlumniCard({ alumn }: AlumniCardProps) {
  const theme = useThemeColors();
  const name = [alumn.firstName, alumn.lastName].filter(Boolean).join(" ");
  const hasSubtitle = !!(alumn.currentPosition || alumn.company);

  return (
    <Card className="gap-5 rounded-2xl p-5">
      <View className="flex-row gap-4">
        <View className="border-border bg-muted size-16 items-center justify-center overflow-hidden rounded-xl border">
          <UserRound size={32} color={theme.mutedForeground} strokeWidth={1.5} />
        </View>
        <View className="flex-1 gap-1.5">
          <View className="gap-0.5">
            <Text className="font-heading text-foreground text-lg font-semibold leading-tight">
              {name}
            </Text>
            {alumn.graduationYear ? (
              <Text className="text-muted-foreground text-[10px] font-semibold uppercase tracking-widest">
                Rocznik {alumn.graduationYear}
              </Text>
            ) : null}
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

      <Button
        variant="outline"
        onPress={() => router.push({ pathname: "/alumni/[id]", params: { id: alumn.id } })}
      >
        <Text className="font-semibold">Zobacz profil</Text>
        <ArrowRight size={16} color={theme.foreground} />
      </Button>
    </Card>
  );
}
