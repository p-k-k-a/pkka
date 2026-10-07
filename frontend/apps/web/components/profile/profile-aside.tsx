import type { LucideIcon } from "lucide-react";

/** Side panel styled like an event's "Lokalizacja i czas" aside. */
export function ProfileAside({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <aside className="border-accent border-l-2 pl-6">
      <p className="text-accent mb-6 text-xs font-semibold tracking-widest uppercase">{title}</p>
      <div className="space-y-5">{children}</div>
    </aside>
  );
}

type ProfileAsideItemProps = {
  icon: LucideIcon;
  label: string;
  children: React.ReactNode;
};

export function ProfileAsideItem({ icon: Icon, label, children }: ProfileAsideItemProps) {
  return (
    <div className="flex items-start gap-3">
      <Icon className="text-muted-foreground mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <div className="min-w-0">
        <p className="text-muted-foreground text-xs">{label}</p>
        <p className="text-foreground text-sm font-semibold break-words">{children}</p>
      </div>
    </div>
  );
}
