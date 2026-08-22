import type { LucideIcon } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type ProfileSectionCardProps = {
  title: string;
  icon?: LucideIcon;
  action?: React.ReactNode;
  description?: string;
  children: React.ReactNode;
  className?: string;
};

export function ProfileSectionCard({
  title,
  icon: Icon,
  action,
  description,
  children,
  className,
}: ProfileSectionCardProps) {
  return (
    <Card className={cn("gap-4 py-6", className)}>
      <CardHeader className="gap-2 px-6">
        <div className="flex items-center justify-between gap-4">
          <CardTitle className="flex items-center gap-2 text-[18px] font-semibold">
            {Icon ? <Icon className="text-muted-foreground size-4" aria-hidden="true" /> : null}
            {title}
          </CardTitle>
          {action}
        </div>
        {description ? (
          <p className="text-muted-foreground text-[13px] leading-relaxed">{description}</p>
        ) : null}
      </CardHeader>
      <CardContent className="px-6">{children}</CardContent>
    </Card>
  );
}
