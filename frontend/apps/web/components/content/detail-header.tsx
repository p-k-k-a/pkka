import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";

type DetailHeaderProps = {
  backHref: string;
  title: string;
};

export function DetailHeader({ backHref, title }: DetailHeaderProps) {
  return (
    <div className="border-border/70 mb-6 flex items-center gap-3 border-b pb-4 md:mb-8">
      <Button
        asChild
        variant="ghost"
        size="icon"
        className="text-muted-foreground hover:text-foreground shrink-0 rounded-full"
      >
        <Link href={backHref}>
          <ArrowLeft className="size-5" />
          <span className="sr-only">Wróć</span>
        </Link>
      </Button>
      <h2 className="text-foreground text-sm font-bold tracking-widest uppercase">{title}</h2>
    </div>
  );
}
