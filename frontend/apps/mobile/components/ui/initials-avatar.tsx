import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { avatarColors, avatarInitials } from "@pkka/domain";
import { View } from "react-native";

const SIZES = {
  md: { box: "size-16", text: "text-lg" },
  lg: { box: "size-20", text: "text-2xl" },
  xl: { box: "size-36", text: "text-4xl" },
};

type InitialsAvatarProps = {
  name: string;
  size?: keyof typeof SIZES;
  className?: string;
};

export function InitialsAvatar({ name, size = "md", className }: InitialsAvatarProps) {
  const color = avatarColors(name);

  return (
    <View
      className={cn(
        "border-border items-center justify-center rounded-full border",
        SIZES[size].box,
        color.bg,
        className,
      )}
    >
      <Text className={cn("font-semibold", SIZES[size].text, color.text)}>
        {avatarInitials(name)}
      </Text>
    </View>
  );
}
