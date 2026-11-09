import { Calendar, Clock } from "lucide-react";
import { formatPublishedAt } from "@pkka/domain";

export function PublishedMeta({ publishedAt }: { publishedAt?: string }) {
  const { dateLabel, timeLabel } = formatPublishedAt(publishedAt);

  return (
    <ul className="text-muted-foreground space-y-3 text-sm">
      <li className="flex items-start gap-3">
        <Calendar className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>{dateLabel}</span>
      </li>
      <li className="flex items-start gap-3">
        <Clock className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>{timeLabel}</span>
      </li>
    </ul>
  );
}
