import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type ProfileSectionCardProps = {
  title: string;
  children: React.ReactNode;
  className?: string;
};

export function ProfileSectionCard({ title, children, className }: ProfileSectionCardProps) {
  return (
    <Card className={cn("gap-3 p-6", className)}>
      <p className="text-muted-foreground text-[10px] font-semibold tracking-widest uppercase">
        {title}
      </p>
      <div>{children}</div>
    </Card>
  );
}
