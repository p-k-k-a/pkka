import { DetailHeader } from "@/components/ui/detail-header";
import { PageHeader } from "@/components/ui/page-header";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import {
  APP_GUIDE_ADMIN_SECTION,
  APP_GUIDE_INTRO,
  APP_GUIDE_SECTIONS,
  APP_GUIDE_STEPS,
  isAdmin,
  type AppGuideIcon,
} from "@pkka/domain";
import { useMe } from "@pkka/api";
import { useAuth } from "@/lib/auth-context";
import { DiscordIcon } from "@pkka/icons/native";
import { router } from "expo-router";
import {
  BadgeCheck,
  Bell,
  CalendarDays,
  FolderOpen,
  Hash,
  Megaphone,
  Newspaper,
  ShieldCheck,
  UserRound,
  Users,
  UsersRound,
} from "lucide-react-native";
import { ScrollView, View } from "react-native";

const ICONS: Record<AppGuideIcon, React.ComponentType<{ size?: number; color?: string }>> = {
  news: Newspaper,
  calendar: CalendarDays,
  users: Users,
  user: UserRound,
  folder: FolderOpen,
  groups: UsersRound,
  bell: Bell,
  shield: ShieldCheck,
  megaphone: Megaphone,
  discord: DiscordIcon,
  badge: BadgeCheck,
  hash: Hash,
};

function SectionTitle({ title, description }: { title: string; description?: string }) {
  return (
    <View className="gap-1">
      <Text
        role="heading"
        aria-level="2"
        className="font-heading text-foreground text-[23px] font-semibold"
      >
        {title}
      </Text>
      {description ? (
        <Text className="text-muted-foreground text-sm leading-5">{description}</Text>
      ) : null}
    </View>
  );
}

export default function InfoScreen() {
  const colors = useThemeColors();
  const { user } = useAuth();
  const { data: me } = useMe({ query: { enabled: !!user } });
  const sections =
    user && isAdmin(me?.data.roles)
      ? [...APP_GUIDE_SECTIONS, APP_GUIDE_ADMIN_SECTION]
      : APP_GUIDE_SECTIONS;

  return (
    <View className="flex-1 bg-background">
      <DetailHeader title="O aplikacji" onBack={() => router.back()} />
      <ScrollView showsVerticalScrollIndicator={false} contentContainerClassName="pb-12">
        <PageHeader eyebrow={APP_GUIDE_INTRO.eyebrow} title={APP_GUIDE_INTRO.title}>
          {APP_GUIDE_INTRO.paragraphs.map((paragraph) => (
            <Text key={paragraph} className="text-muted-foreground text-base leading-relaxed">
              {paragraph}
            </Text>
          ))}
        </PageHeader>

        <View className="gap-10 px-5 pt-8">
          <View className="gap-4">
            <SectionTitle title="Jak zacząć" />
            {APP_GUIDE_STEPS.map((step, index) => (
              <View key={step.title} className="flex-row gap-4">
                <View className="bg-primary size-8 items-center justify-center rounded-full">
                  <Text className="text-primary-foreground font-heading text-sm font-semibold">
                    {index + 1}
                  </Text>
                </View>
                <View className="flex-1 gap-1">
                  <Text className="text-foreground text-base font-semibold">{step.title}</Text>
                  <Text className="text-muted-foreground text-sm leading-5">
                    {step.description}
                  </Text>
                </View>
              </View>
            ))}
          </View>

          {sections.map((section) => (
            <View key={section.title} className="gap-4">
              <SectionTitle title={section.title} description={section.description} />
              {section.features.map((feature) => {
                const Icon = ICONS[feature.icon];
                return (
                  <View key={feature.title} className="flex-row gap-4">
                    <View className="bg-muted size-9 items-center justify-center rounded-lg">
                      <Icon size={18} color={colors.foreground} />
                    </View>
                    <View className="flex-1 gap-0.5">
                      <Text className="text-foreground text-base font-semibold">
                        {feature.title}
                      </Text>
                      <Text className="text-muted-foreground text-sm leading-5">
                        {feature.description}
                      </Text>
                    </View>
                  </View>
                );
              })}
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}
