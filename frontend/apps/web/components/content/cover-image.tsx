import Image from "next/image";
import { ImageIcon } from "lucide-react";
import { DEFAULT_COVER_IMAGE } from "@/lib/content-images";
import { cn } from "@/lib/utils";

type CoverImageProps = {
  src?: string | null;
  alt: string;
  aspect?: "video" | "photo";
  showPlaceholder?: boolean;
  /** Serve the image as it is, for hosts the image optimizer is not configured for. */
  unoptimized?: boolean;
  className?: string;
};

export function CoverImage({
  src,
  alt,
  aspect = "video",
  showPlaceholder = false,
  unoptimized = false,
  className,
}: CoverImageProps) {
  const imageSrc = src ?? (showPlaceholder ? null : DEFAULT_COVER_IMAGE);
  const aspectClass = aspect === "photo" ? "aspect-[4/3]" : "aspect-[16/9]";

  return (
    <div
      className={cn(
        "bg-muted relative mb-6 flex w-full items-center justify-center overflow-hidden rounded-lg md:mb-8",
        aspectClass,
        className,
      )}
    >
      {imageSrc ? (
        <Image
          src={imageSrc}
          alt={alt}
          fill
          priority
          unoptimized={unoptimized}
          sizes="(max-width: 768px) 100vw, 768px"
          className="object-cover"
        />
      ) : (
        <ImageIcon className="text-border size-14" aria-hidden="true" />
      )}
    </div>
  );
}
