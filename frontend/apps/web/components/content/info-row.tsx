type InfoRowProps = {
  icon: React.ReactNode;
  label?: string;
  value: string;
  sub?: string;
};

export function InfoRow({ icon, label, value, sub }: InfoRowProps) {
  return (
    <div className="flex items-start gap-3">
      <div className="bg-muted flex size-10 shrink-0 items-center justify-center rounded-md">
        {icon}
      </div>
      <div className="min-w-0 space-y-0.5">
        {label ? (
          <p className="text-muted-foreground text-[10px] font-semibold tracking-widest uppercase">
            {label}
          </p>
        ) : null}
        <p className="text-foreground text-sm font-bold">{value}</p>
        {sub ? <p className="text-muted-foreground text-xs">{sub}</p> : null}
      </div>
    </div>
  );
}
