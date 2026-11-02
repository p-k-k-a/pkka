import { InitialsAvatar } from "@/components/ui/initials-avatar";
import { Badge } from "@/components/ui/badge";
import { Text } from "@/components/ui/text";
import type { AlumnProfile } from "@/components/alumni/alumni-profile-view";
import { useThemeColors } from "@/lib/theme";
import { HeartHandshake } from "lucide-react-native";
import { View } from "react-native";

type ProfileHeroProps = {
  profile: AlumnProfile;
};

export function ProfileHero({ profile }: ProfileHeroProps) {
  const theme = useThemeColors();
  const name = [profile.firstName, profile.lastName].filter(Boolean).join(" ");
  const alumnSinceDate = profile.alumnSince?.split("-").reverse().join(".");

  return (
    <View className="gap-6">
      <View className="gap-1">
        {profile.visibility.name && name ? (
          <View className="flex-row items-center gap-4">
            <InitialsAvatar name={name} size="lg" />
            <Text
              role="heading"
              aria-level="1"
              className="font-heading text-foreground flex-1 text-[33px] font-semibold leading-tight tracking-tight"
            >
              {name}
            </Text>
          </View>
        ) : null}
        {alumnSinceDate ? (
          <Text className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
            Alumn od {alumnSinceDate}
          </Text>
        ) : null}
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
        {profile.willingToMentor ? (
          <Badge className="mt-2 gap-1.5 self-start px-3 py-1">
            <HeartHandshake size={13} color={theme.primaryForeground} />
            <Text className="text-xs font-semibold uppercase tracking-wider">
              Otwarty na mentoring
            </Text>
          </Badge>
        ) : null}
      </View>
    </View>
  );
}
