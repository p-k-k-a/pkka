import type { LucideIcon } from "lucide-react";

type LabeledDetail = { icon: LucideIcon; label: string; value: string };

export function LabeledDetails({ items }: { items: LabeledDetail[] }) {
  return (
    <dl className="space-y-3 text-sm">
      {items.map(({ icon: Icon, label, value }) => (
        <div key={label} className="flex items-start gap-3">
          <dt className="text-muted-foreground flex shrink-0 items-start gap-3">
            <Icon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            {label}:
          </dt>
          <dd className="text-foreground min-w-0 break-words">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
