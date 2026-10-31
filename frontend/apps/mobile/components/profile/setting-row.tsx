import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import type { LucideIcon } from "lucide-react-native";
import type { ReactNode } from "react";
import { View } from "react-native";

function SettingIcon({ icon: Icon }: { icon: LucideIcon }) {
  const theme = useThemeColors();
  return (
    <View className="bg-muted size-9 items-center justify-center rounded-lg">
      <Icon size={18} color={theme.mutedForeground} />
    </View>
  );
}

export function SettingRow({
  icon,
  title,
  description,
  action,
}: {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <View className="flex-row items-center gap-3">
      <SettingIcon icon={icon} />
      <View className="flex-1 gap-0.5">
        <Text className="text-foreground text-base font-semibold">{title}</Text>
        {description ? (
          <Text className="text-muted-foreground text-sm leading-5">{description}</Text>
        ) : null}
      </View>
      {action}
    </View>
  );
}
