import { MaterialCard } from "@/components/materials/material-card";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import { useListMaterials } from "@pkka/api";
import { Link } from "expo-router";
import { ArrowRight } from "lucide-react-native";
import { Pressable, View } from "react-native";

const PREVIEW_SIZE = 2;

type EventMaterialsSectionProps = {
  eventId: string;
  eventTitle: string;
};

export function EventMaterialsSection({ eventId, eventTitle }: EventMaterialsSectionProps) {
  const theme = useThemeColors();
  const { user } = useAuth();
  const { data } = useListMaterials(
    { eventId, size: PREVIEW_SIZE },
    { query: { enabled: !!user } },
  );
  const materials = data?.data.content ?? [];

  if (!user || materials.length === 0) return null;

  const total = data?.data.totalElements ?? materials.length;

  return (
    <>
      <View className="bg-border h-px" />
      <View className="gap-4">
        <Text
          role="heading"
          aria-level="2"
          className="font-heading text-foreground text-[23px] font-semibold leading-tight"
        >
          Materiały
        </Text>
        {materials.map((material) => (
          <MaterialCard key={material.id} material={material} showEvent={false} />
        ))}
        <Link href={{ pathname: "/materials", params: { eventId, eventTitle } }} asChild>
          <Pressable className="flex-row items-center gap-1.5 self-start py-1 active:opacity-70">
            <Text className="text-accent text-sm font-medium">
              {total > materials.length
                ? `Wszystkie materiały (${total})`
                : "Przejdź do materiałów"}
            </Text>
            <ArrowRight size={14} color={theme.accent} />
          </Pressable>
        </Link>
      </View>
    </>
  );
}
