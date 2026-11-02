import { InitialsAvatar } from "@/components/ui/initials-avatar";
import { SkillChips } from "@/components/alumni/skill-chips";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import type { AlumniListItemResponse } from "@pkka/api";
import { router } from "expo-router";
import { ArrowRight } from "lucide-react-native";
import { Pressable, View } from "react-native";

type AlumniCardProps = {
  alumn: AlumniListItemResponse;
};

export function AlumniCard({ alumn }: AlumniCardProps) {
  const theme = useThemeColors();
  const name = [alumn.firstName, alumn.lastName].filter(Boolean).join(" ");
  const hasSubtitle = !!(alumn.currentPosition || alumn.company);

  return (
    <View className="bg-muted gap-5 rounded-2xl p-6">
      <View className="flex-row gap-4">
        <InitialsAvatar name={name} />
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

      <Pressable
        className="bg-background flex-row items-center gap-1.5 self-end rounded-lg px-3 py-2 active:opacity-80"
        onPress={() => router.push({ pathname: "/alumni/[id]", params: { id: alumn.id } })}
      >
        <Text className="text-accent text-sm font-medium">Zobacz profil</Text>
        <ArrowRight size={14} color={theme.accent} />
      </Pressable>
    </View>
  );
}
