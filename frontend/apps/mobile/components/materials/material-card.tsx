import { Text } from "@/components/ui/text";
import { openWithFallback } from "@/lib/contact";
import { useThemeColors } from "@/lib/theme";
import { MaterialType, type MaterialResponse } from "@pkka/api";
import { materialLinkHost, materialTypeLabelUpper, youtubeThumbnailUrl } from "@pkka/domain";
import { Image } from "expo-image";
import {
  ArrowUpRight,
  Calendar,
  FileText,
  Link2,
  Play,
  PlayCircle,
  type LucideIcon,
} from "lucide-react-native";
import { Pressable, View } from "react-native";

const TYPE_ICONS: Record<MaterialType, LucideIcon> = {
  RECORDING: PlayCircle,
  PRESENTATION: FileText,
  OTHER: Link2,
};

type MaterialCardProps = {
  material: MaterialResponse;
  showEvent?: boolean;
};

export function MaterialCard({ material, showEvent = true }: MaterialCardProps) {
  const theme = useThemeColors();
  const { title, description, type, url, eventTitle } = material;
  const thumbnail = youtubeThumbnailUrl(url);
  const host = materialLinkHost(url);
  const TypeIcon = TYPE_ICONS[type];

  return (
    <Pressable
      role="link"
      accessibilityLabel={title}
      accessibilityHint={host ? `Otwiera ${host}` : "Otwiera link"}
      onPress={() => void openWithFallback(url, url)}
      className="bg-muted overflow-hidden rounded-2xl active:opacity-90"
    >
      {thumbnail ? (
        <View className="items-center justify-center">
          <Image
            source={{ uri: thumbnail }}
            contentFit="cover"
            transition={200}
            style={{ width: "100%", aspectRatio: 16 / 9 }}
          />
          <View className="absolute size-12 items-center justify-center rounded-full bg-black/55 pl-0.5">
            <Play size={20} color="#ffffff" fill="#ffffff" />
          </View>
        </View>
      ) : null}

      <View className="gap-4 p-6">
        <View className="bg-band flex-row items-center gap-1.5 self-start rounded-lg px-3 py-1">
          <TypeIcon size={12} color={theme.bandForeground} />
          <Text className="text-band-foreground text-[11px] font-semibold uppercase tracking-widest">
            {materialTypeLabelUpper(type)}
          </Text>
        </View>

        <View className="gap-2">
          <Text
            role="heading"
            aria-level="3"
            className="font-heading text-foreground text-lg font-semibold leading-snug"
          >
            {title}
          </Text>
          {description ? (
            <Text numberOfLines={3} className="text-muted-foreground text-sm leading-relaxed">
              {description}
            </Text>
          ) : null}
        </View>

        {showEvent && eventTitle ? (
          <View className="flex-row items-start gap-3">
            <Calendar size={16} color={theme.mutedForeground} style={{ marginTop: 2 }} />
            <Text className="text-muted-foreground flex-1 text-sm">{eventTitle}</Text>
          </View>
        ) : null}

        <View className="bg-background flex-row items-center gap-1.5 self-end rounded-lg px-3 py-2">
          <Text className="text-accent text-sm font-medium">
            {host ? `Otwórz w ${host}` : "Otwórz"}
          </Text>
          <ArrowUpRight size={14} color={theme.accent} />
        </View>
      </View>
    </Pressable>
  );
}
