"use client";

import * as React from "react";
import { avatarColors, avatarInitials } from "@pkka/domain";
import { cn } from "@/lib/utils";

interface AvatarProps extends React.HTMLAttributes<HTMLDivElement> {
  src?: string;
  alt?: string;
  fallback?: string;
  size?: "sm" | "md" | "lg" | "xl";
}

export function Avatar({
  src,
  alt = "",
  fallback = "",
  size = "md",
  className,
  ...props
}: AvatarProps) {
  // Remembered per URL, so a new picture gets its own chance after an old one failed to load.
  const [failedSrc, setFailedSrc] = React.useState<string | null>(null);

  const sizeClasses = {
    sm: "size-8 text-xs font-semibold",
    md: "size-10 text-sm font-semibold",
    lg: "size-12 text-base font-semibold",
    xl: "size-16 text-lg font-semibold",
  };

  const showFallback = !src || failedSrc === src;

  return (
    <div
      className={cn(
        "bg-muted relative flex shrink-0 items-center justify-center overflow-hidden rounded-full border select-none",
        sizeClasses[size],
        showFallback && avatarColors(fallback || alt).bg,
        showFallback && avatarColors(fallback || alt).text,
        className,
      )}
      {...props}
    >
      {!showFallback ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={src}
          alt={alt}
          onError={() => setFailedSrc(src)}
          className="aspect-square h-full w-full object-cover"
        />
      ) : (
        <span>{avatarInitials(fallback || alt)}</span>
      )}
    </div>
  );
}
