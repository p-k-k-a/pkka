import { cn } from "@/lib/utils";

export function DetailList({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return <dl className={cn("divide-border divide-y", className)}>{children}</dl>;
}

type DetailRowProps = {
  label: string;
  value?: string | number | null;
  icon?: React.ReactNode;
};

export function DetailRow({ label, value, icon }: DetailRowProps) {
  if (value === null || value === undefined || value === "") return null;

  return (
    <div className="flex items-baseline justify-between gap-4 py-2 first:pt-0 last:pb-0">
      <dt className="text-muted-foreground text-[13px]">{label}</dt>
      <dd className="text-foreground flex items-center gap-1.5 text-sm font-semibold">
        {icon}
        {value}
      </dd>
    </div>
  );
}
