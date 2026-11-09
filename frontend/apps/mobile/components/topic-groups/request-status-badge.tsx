import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { cn } from "@/lib/utils";
import { TopicGroupRequestStatus } from "@pkka/api";
import { topicGroupRequestStatusLabel } from "@pkka/domain";
import { CircleCheck, CircleX, Clock, type LucideIcon } from "lucide-react-native";
import { View } from "react-native";

const STATUS_STYLE: Record<
  TopicGroupRequestStatus,
  { icon: LucideIcon; className: string; color: "primary" | "accent" | "destructive" }
> = {
  UNDER_REVIEW: { icon: Clock, className: "bg-primary/15", color: "primary" },
  APPROVED: { icon: CircleCheck, className: "bg-accent/15", color: "accent" },
  REJECTED: { icon: CircleX, className: "bg-destructive/15", color: "destructive" },
};

export function RequestStatusBadge({ status }: { status: TopicGroupRequestStatus }) {
  const theme = useThemeColors();
  const { icon: Icon, className, color } = STATUS_STYLE[status];

  return (
    <View className={cn("flex-row items-center gap-1.5 rounded-md px-2.5 py-1", className)}>
      <Icon size={14} color={theme[color]} />
      <Text className="text-foreground text-xs font-semibold">
        {topicGroupRequestStatusLabel(status)}
      </Text>
    </View>
  );
}
