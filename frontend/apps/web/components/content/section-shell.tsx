type SectionShellProps = {
  title: string;
  description?: string;
  as?: "section" | "div";
  tone?: "muted" | "background";
  children: React.ReactNode;
};

export function SectionShell({
  title,
  description,
  as: Tag = "div",
  tone = "background",
  children,
}: SectionShellProps) {
  return (
    <Tag
      className={
        tone === "muted"
          ? "bg-muted px-4 py-10 md:px-10 md:py-20"
          : "bg-background px-4 py-10 md:px-10 md:py-20"
      }
    >
      <div className="mx-auto max-w-[1280px]">
        <div className="mb-10 space-y-3">
          <h2 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            {title}
          </h2>
          {description ? (
            <p className="text-muted-foreground max-w-3xl text-base leading-relaxed">{description}</p>
          ) : null}
        </div>
        {children}
      </div>
    </Tag>
  );
}
