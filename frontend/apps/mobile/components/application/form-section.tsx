import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type FormSectionProps = {
  title: string;
  description?: string;
  className?: string;
  children: React.ReactNode;
};

function FormSection({ title, description, className, children }: FormSectionProps) {
  return (
    <Card className={cn("gap-5", className)}>
      <CardHeader>
        <CardTitle className="text-xl font-extrabold uppercase tracking-tight">{title}</CardTitle>
        {description ? (
          <CardDescription className="leading-5">{description}</CardDescription>
        ) : null}
      </CardHeader>
      <CardContent className="gap-5">{children}</CardContent>
    </Card>
  );
}

export { FormSection };
