import Image from "next/image";
import { ImageIcon } from "lucide-react";
import { DEFAULT_COVER_IMAGE } from "@/lib/content-images";

type CoverImageProps = {
  src?: string | null;
  alt: string;
  aspect?: "video" | "photo";
  showPlaceholder?: boolean;
  className?: string;
};

export function CoverImage({
  src,
  alt,
  aspect = "video",
  showPlaceholder = false,
  className,
}: CoverImageProps) {
  const imageSrc = src ?? (showPlaceholder ? null : DEFAULT_COVER_IMAGE);
  const aspectClass = aspect === "photo" ? "aspect-[4/3]" : "aspect-[16/9]";

  return (
    <div
      className={`border-border bg-muted relative mb-6 flex ${aspectClass} w-full items-center justify-center overflow-hidden rounded-xl border md:mb-8 ${className ?? ""}`}
    >
      {imageSrc ? (
        <Image
          src={imageSrc}
          alt={alt}
          fill
          priority
          sizes="(max-width: 768px) 100vw, 768px"
          className="object-cover"
        />
      ) : (
        <ImageIcon className="text-border size-14" aria-hidden="true" />
      )}
    </div>
  );
}
