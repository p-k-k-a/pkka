import { AVATAR_BOX_SIZES, InitialsAvatar } from "@/components/ui/initials-avatar";
import { cn } from "@/lib/utils";
import { Image } from "expo-image";
import { useState } from "react";
import { View } from "react-native";

type UserAvatarProps = {
  /** The profile picture; initials are shown without one, or when it fails to load. */
  imageUrl?: string | null;
  name: string;
  size?: keyof typeof AVATAR_BOX_SIZES;
  className?: string;
};

export function UserAvatar({ imageUrl, name, size = "md", className }: UserAvatarProps) {
  // Remembered per URL, so a new picture gets its own chance after an old one failed to load.
  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  if (!imageUrl || failedUrl === imageUrl) {
    return <InitialsAvatar name={name} size={size} className={className} />;
  }

  return (
    <View
      className={cn(
        "border-border bg-muted overflow-hidden rounded-full border",
        AVATAR_BOX_SIZES[size],
        className,
      )}
    >
      <Image
        source={{ uri: imageUrl }}
        contentFit="cover"
        transition={150}
        accessibilityLabel="Zdjęcie profilowe"
        style={{ width: "100%", height: "100%" }}
        onError={() => setFailedUrl(imageUrl)}
      />
    </View>
  );
}
