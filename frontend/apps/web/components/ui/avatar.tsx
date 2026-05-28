import * as React from "react";
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
  const [hasError, setHasError] = React.useState(false);

  const sizeClasses = {
    sm: "size-8 text-xs font-semibold",
    md: "size-10 text-sm font-semibold",
    lg: "size-12 text-base font-semibold",
    xl: "size-16 text-lg font-semibold",
  };

  const getInitials = (name: string) => {
    if (!name) return "";
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  const colors = [
    "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300",
    "bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-300",
    "bg-indigo-100 text-indigo-800 dark:bg-indigo-950 dark:text-indigo-300",
    "bg-violet-100 text-violet-800 dark:bg-violet-950 dark:text-violet-300",
    "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300",
    "bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300",
  ];

  const getColorClass = (name: string) => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % colors.length;
    return colors[index];
  };

  const showFallback = !src || hasError;

  return (
    <div
      className={cn(
        "relative flex shrink-0 overflow-hidden rounded-full items-center justify-center bg-muted border select-none",
        sizeClasses[size],
        showFallback && getColorClass(fallback || alt),
        className
      )}
      {...props}
    >
      {!showFallback ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={src}
          alt={alt}
          onError={() => setHasError(true)}
          className="aspect-square h-full w-full object-cover"
        />
      ) : (
        <span>{getInitials(fallback || alt)}</span>
      )}
    </div>
  );
}
