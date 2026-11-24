import { cn } from "@/lib/utils";
import { Image } from "expo-image";
import { useState } from "react";
import { View } from "react-native";

type CoverImageProps = {
  /** The cover an admin set. Nothing is shown without one, or when it fails to load. */
  imageUrl?: string | null;
  className?: string;
};

export function CoverImage({ imageUrl, className }: CoverImageProps) {
  // Remembered per URL, so a new cover gets its own chance after an old one failed to load.
  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  if (!imageUrl?.startsWith("http") || failedUrl === imageUrl) return null;

  // A signed URL is cached by the file: its signature changes about every hour, and each new one
  // would otherwise download the same picture again.
  const cacheKey = imageUrl.includes("X-Amz-Signature=") ? imageUrl.split("?")[0] : undefined;

  return (
    <View className={cn("bg-muted w-full overflow-hidden", className)}>
      <Image
        source={{ uri: imageUrl, cacheKey }}
        contentFit="cover"
        transition={200}
        style={{ width: "100%", height: "100%" }}
        onError={() => setFailedUrl(imageUrl)}
      />
    </View>
  );
}
