/** The highlighted section label used on event pages ("O wydarzeniu"). */
export function SectionHeading({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative inline-block">
      <span className="bg-muted absolute inset-x-0 bottom-0 h-3" aria-hidden="true" />
      <h2 className="text-accent relative text-xs font-semibold tracking-widest uppercase">
        {children}
      </h2>
    </div>
  );
}
