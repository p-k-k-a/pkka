import { cn } from "@/lib/utils";

export function CharacterCount({ value, limit }: { value: string; limit: number }) {
  const isNearLimit = value.length > limit * 0.9;
  return (
    <p
      className={cn(
        "text-muted-foreground text-right text-xs tabular-nums",
        isNearLimit && "text-destructive",
      )}
    >
      {value.length}/{limit}
    </p>
  );
}
