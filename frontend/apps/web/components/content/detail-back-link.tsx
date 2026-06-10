import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type DetailBackLinkProps = {
  href: string;
  label: string;
  className?: string;
};

export function DetailBackLink({ href, label, className }: DetailBackLinkProps) {
  return (
    <Button asChild variant="outline" className={cn("rounded-xl font-semibold", className)}>
      <Link href={href}>
        <ArrowLeft className="mr-2 size-4" />
        {label}
      </Link>
    </Button>
  );
}
