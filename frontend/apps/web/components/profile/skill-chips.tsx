import type { UserTagResponse } from "@pkka/api";
import { Badge } from "@/components/ui/badge";

type SkillChipsProps = {
  tags: UserTagResponse[];
};

export function SkillChips({ tags }: SkillChipsProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {tags.map((tag) => (
        <Badge
          key={tag.id}
          variant="secondary"
          className="bg-muted text-foreground rounded-md px-3 py-1 text-xs font-semibold uppercase"
        >
          {tag.name}
        </Badge>
      ))}
    </div>
  );
}
