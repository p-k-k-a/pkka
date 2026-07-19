import { ContactActions } from "@/components/alumni/contact-actions";
import { ProfileHero } from "@/components/alumni/profile-hero";
import { ProfileSectionCard } from "@/components/alumni/profile-section-card";
import { SkillChips } from "@/components/alumni/skill-chips";
import { Text } from "@/components/ui/text";
import type { AlumnProfile } from "@/lib/alumni-mock";
import { THEME } from "@/lib/theme";
import { Pencil } from "lucide-react-native";
import { Pressable, View } from "react-native";

type AlumniProfileViewProps = {
  profile: AlumnProfile;
  onEdit: () => void;
};

export function AlumniProfileView({ profile, onEdit }: AlumniProfileViewProps) {
  return (
    <View className="gap-6">
      <Pressable
        onPress={onEdit}
        role="button"
        accessibilityLabel="Edytuj profil"
        className="border-border active:bg-muted flex-row items-center gap-2 self-start rounded-full border px-3 py-1.5"
      >
        <Pencil size={14} color={THEME.light.foreground} />
        <Text className="text-foreground text-xs font-semibold uppercase tracking-wider">
          Edytuj profil
        </Text>
      </Pressable>

      <ProfileHero profile={profile} />
      <ContactActions profile={profile} />

      {profile.bio ? (
        <ProfileSectionCard title="O mnie">
          <Text className="leading-7 text-muted-foreground">{profile.bio}</Text>
        </ProfileSectionCard>
      ) : null}

      {profile.tags.length > 0 ? (
        <ProfileSectionCard title="Umiejętności">
          <SkillChips tags={profile.tags} />
        </ProfileSectionCard>
      ) : null}
    </View>
  );
}
