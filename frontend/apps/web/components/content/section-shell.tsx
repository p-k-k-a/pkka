import { cn } from "@/lib/utils";

type SectionShellProps = {
  title: string;
  description?: string;
  as?: "section" | "div";
  tone?: "muted" | "background";
  align?: "start" | "center";
  id?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
};

export function SectionShell({
  title,
  description,
  as: Tag = "div",
  tone = "background",
  align = "start",
  id,
  action,
  children,
}: SectionShellProps) {
  return (
    <Tag
      id={id}
      className={cn(
        "scroll-mt-16 px-4 py-10 md:px-10 md:py-20",
        tone === "muted" ? "bg-muted" : "bg-background",
      )}
    >
      <div className="mx-auto max-w-[1280px]">
        <div
          className={cn(
            "mb-10 flex flex-col gap-6 md:flex-row md:items-end md:justify-between",
            align === "center" && "text-center md:justify-center",
          )}
        >
          <div className="space-y-3">
            <h2 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              {title}
            </h2>
            {description ? (
              <p
                className={cn(
                  "text-muted-foreground max-w-3xl text-base leading-relaxed",
                  align === "center" && "mx-auto",
                )}
              >
                {description}
              </p>
            ) : null}
          </div>
          {action ? <div className="shrink-0">{action}</div> : null}
        </div>
        {children}
      </div>
    </Tag>
  );
}
