type SectionShellProps = {
  title: string;
  description?: string;
  as?: "section" | "div";
  children: React.ReactNode;
};

export function SectionShell({
  title,
  description,
  as: Tag = "div",
  children,
}: SectionShellProps) {
  return (
    <Tag className="mx-auto max-w-7xl px-6 py-12 md:py-16">
      <div className="mb-10 space-y-3">
        <h2 className="text-foreground text-3xl font-extrabold tracking-tight md:text-4xl">
          {title}
        </h2>
        {description ? (
          <p className="text-muted-foreground max-w-3xl leading-relaxed">{description}</p>
        ) : null}
      </div>
      {children}
    </Tag>
  );
}
