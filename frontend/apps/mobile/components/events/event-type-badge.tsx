import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { EventType } from "@pkka/api";
import { eventTypeLabelUpper } from "@pkka/domain";
import { View } from "react-native";

const EVENT_TYPE_BADGE: Record<EventType, string> = {
  IN_PERSON: "bg-[#4cc38a]",
  ONLINE: "bg-[#188ef2]",
  HYBRID: "bg-primary",
};

export function EventTypeBadge({ type, className }: { type: EventType; className?: string }) {
  return (
    <View className={cn("rounded-lg px-3 py-1", EVENT_TYPE_BADGE[type], className)}>
      <Text className="text-primary-foreground text-[11px] font-semibold uppercase tracking-widest">
        {eventTypeLabelUpper(type)}
      </Text>
    </View>
  );
}
