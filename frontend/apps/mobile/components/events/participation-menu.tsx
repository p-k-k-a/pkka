import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Icon } from "@/components/ui/icon";
import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { ChevronUp, type LucideIcon } from "lucide-react-native";
import { Fragment } from "react";
import { Pressable, View, useWindowDimensions } from "react-native";
import Animated, { useAnimatedStyle, useSharedValue, withTiming } from "react-native-reanimated";

export type ParticipationMenuItem = {
  label: string;
  icon: LucideIcon;
  onPress: () => void;
  destructive?: boolean;
};

type ParticipationMenuProps = {
  label: string;
  icon: LucideIcon;
  items: ParticipationMenuItem[];
  header?: { title: string; description: string };
  disabled?: boolean;
};

export function ParticipationMenu({
  label,
  icon,
  items,
  header,
  disabled,
}: ParticipationMenuProps) {
  const { width: screenWidth } = useWindowDimensions();
  const rotation = useSharedValue(0);
  const chevronStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${rotation.value}deg` }],
  }));

  return (
    <DropdownMenu
      className="flex-1"
      onOpenChange={(open) => {
        rotation.value = withTiming(open ? 180 : 0, { duration: 150 });
      }}
    >
      <DropdownMenuTrigger asChild disabled={disabled}>
        <Pressable
          className={cn(
            "border-primary bg-primary/15 active:bg-primary/25 min-h-10 flex-row items-center justify-between gap-2 rounded-md border px-4 py-2",
            disabled && "opacity-50",
          )}
        >
          <View className="flex-row items-center gap-2">
            <Icon as={icon} size={16} className="text-foreground" />
            <Text numberOfLines={1} className="text-foreground text-sm font-semibold">
              {label}
            </Text>
          </View>
          <Animated.View style={chevronStyle}>
            <Icon as={ChevronUp} size={16} className="text-foreground" />
          </Animated.View>
        </Pressable>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        side="top"
        align="start"
        sideOffset={8}
        overlayClassName="bg-black/20"
        style={{ width: screenWidth - 40 }}
        className="bg-card border-muted-foreground/25 dark:border-input rounded-xl"
      >
        {header ? (
          <>
            <View className="gap-1 px-3 py-2">
              <Text className="text-foreground text-sm font-semibold">{header.title}</Text>
              <Text className="text-muted-foreground text-xs">{header.description}</Text>
            </View>
            <DropdownMenuSeparator />
          </>
        ) : null}
        {items.map((item, index) => (
          <Fragment key={item.label}>
            {index > 0 ? <DropdownMenuSeparator /> : null}
            <DropdownMenuItem
              variant={item.destructive ? "destructive" : "default"}
              onPress={item.onPress}
              className="gap-3 px-3 py-3"
            >
              <Icon
                as={item.icon}
                size={18}
                className={
                  item.destructive ? "text-destructive dark:text-foreground" : "text-accent"
                }
              />
              <Text className={cn(item.destructive && "dark:text-foreground")}>{item.label}</Text>
            </DropdownMenuItem>
          </Fragment>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
