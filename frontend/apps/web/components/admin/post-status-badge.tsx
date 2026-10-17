import { PostStatus } from "@pkka/api";
import { cn } from "@/lib/utils";

type PostStatusBadgeProps = {
  status: PostStatus;
};

export function PostStatusBadge({ status }: PostStatusBadgeProps) {
  const published = status === PostStatus.PUBLISHED;

  return (
    <span
      className={cn(
        "inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase",
        published ? "bg-band text-band-foreground" : "bg-accent/10 text-accent",
      )}
    >
      {published ? "Opublikowany" : "Szkic"}
    </span>
  );
}
