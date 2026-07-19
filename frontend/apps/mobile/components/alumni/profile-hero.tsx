import { Text } from "@/components/ui/text";
import type { AlumnProfile } from "@/lib/alumni-mock";
import { THEME } from "@/lib/theme";
import { UserRound } from "lucide-react-native";
import { View } from "react-native";

type ProfileHeroProps = {
  profile: AlumnProfile;
};

export function ProfileHero({ profile }: ProfileHeroProps) {
  const name = `${profile.firstName} ${profile.lastName}`;

  return (
    <View className="gap-6">
      <View className="aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-xl border border-border bg-muted">
        <UserRound size={96} color={THEME.light.mutedForeground} strokeWidth={1.5} />
      </View>

      <View className="gap-1">
        {profile.visibility.name ? (
          <Text variant="h1" className="text-left font-heading leading-tight">
            {name}
          </Text>
        ) : null}
        <Text className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
          Alumn od {profile.alumnSince}
        </Text>
        {profile.currentPosition || profile.company ? (
          <Text className="text-lg text-muted-foreground">
            {profile.currentPosition}
            {profile.currentPosition && profile.company ? " @ " : ""}
            {profile.company ? (
              <Text className="text-lg font-semibold text-foreground">{profile.company}</Text>
            ) : null}
          </Text>
        ) : null}
        {profile.fieldOfStudy && profile.graduationYear ? (
          <Text className="text-sm text-muted-foreground">
            Absolwent: {profile.fieldOfStudy} - {profile.graduationYear}
          </Text>
        ) : null}
      </View>
    </View>
  );
}
