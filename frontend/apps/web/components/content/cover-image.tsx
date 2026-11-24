import Image from "next/image";
import { cn } from "@/lib/utils";

type CoverImageProps = {
  src: string;
  alt: string;
  aspect?: "video" | "photo";
  /**
   * Load the image straight from its URL instead of through the image optimizer. Needed for
   * a cover an admin set: it is either a signed URL of a private file, which must not be
   * re-served from our cache, or a link to a host the optimizer is not configured for.
   */
  unoptimized?: boolean;
  className?: string;
};

export function CoverImage({
  src,
  alt,
  aspect = "video",
  unoptimized = false,
  className,
}: CoverImageProps) {
  const aspectClass = aspect === "photo" ? "aspect-[4/3]" : "aspect-[16/9]";

  return (
    <div
      className={cn(
        "bg-muted relative mb-6 flex w-full items-center justify-center overflow-hidden rounded-lg md:mb-8",
        aspectClass,
        className,
      )}
    >
      <Image
        src={src}
        alt={alt}
        fill
        priority
        unoptimized={unoptimized}
        sizes="(max-width: 768px) 100vw, 768px"
        className="object-cover"
      />
    </div>
  );
}
