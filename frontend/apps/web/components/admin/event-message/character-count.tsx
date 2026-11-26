import { cn } from "@/lib/utils";

export function CharacterCount({
  value,
  limit,
  className,
}: {
  value: string;
  limit: number;
  className?: string;
}) {
  const isNearLimit = value.length > limit * 0.9;
  return (
    <p
      className={cn(
        "text-muted-foreground text-right text-xs tabular-nums",
        className,
        isNearLimit && "text-destructive",
      )}
    >
      {value.length}/{limit}
    </p>
  );
}
