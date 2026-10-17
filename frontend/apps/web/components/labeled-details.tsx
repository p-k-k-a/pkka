import type { LucideIcon } from "lucide-react";

export type LabeledDetail = { icon: LucideIcon; label: string; value: string };

export function LabeledDetails({ items }: { items: LabeledDetail[] }) {
  return (
    <dl className="space-y-3 text-sm">
      {items.map(({ icon: Icon, label, value }) => (
        <div key={label} className="flex items-start gap-3">
          <Icon className="text-muted-foreground mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <dt className="text-muted-foreground shrink-0">{label}:</dt>
          <dd className="text-foreground min-w-0 break-words">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
