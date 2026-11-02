import { ProfileSectionCard } from "@/components/alumni/profile-section-card";
import { NotificationToggle } from "@/components/profile/notification-toggle";
import { SettingRow } from "@/components/profile/setting-row";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import {
  setThemePreference,
  useThemeColors,
  useThemePreference,
  type ThemePreference,
} from "@/lib/theme";
import { cn } from "@/lib/utils";
import { Moon, Smartphone, Sun, type LucideIcon } from "lucide-react-native";
import { Pressable, View } from "react-native";

const THEME_OPTIONS: { value: ThemePreference; label: string; icon: LucideIcon }[] = [
  { value: "light", label: "Jasny", icon: Sun },
  { value: "dark", label: "Ciemny", icon: Moon },
  { value: "system", label: "System", icon: Smartphone },
];

function ThemeSwitch() {
  const theme = useThemeColors();
  const preference = useThemePreference();

  return (
    <View className="bg-muted dark:bg-background flex-row rounded-lg p-1">
      {THEME_OPTIONS.map(({ value, label, icon: Icon }) => {
        const selected = preference === value;
        return (
          <Pressable
            key={value}
            role="radio"
            accessibilityState={{ checked: selected }}
            onPress={() => void setThemePreference(value)}
            className={cn(
              "flex-1 flex-row items-center justify-center gap-1.5 rounded-md py-2",
              selected && "bg-background dark:bg-muted",
            )}
          >
            <Icon size={14} color={selected ? theme.foreground : theme.mutedForeground} />
            <Text
              className={cn(
                "text-[13px] font-semibold",
                selected ? "text-foreground" : "text-muted-foreground",
              )}
            >
              {label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export function SettingsSection() {
  return (
    <ProfileSectionCard title="Ustawienia">
      <View className="gap-4">
        <View className="gap-3">
          <SettingRow icon={Moon} title="Wygląd" />
          <ThemeSwitch />
        </View>
        <Separator />
        <NotificationToggle />
      </View>
    </ProfileSectionCard>
  );
}
