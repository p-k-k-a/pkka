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
    <Button asChild variant="ghost" size="sm" className={cn("-ml-2", className)}>
      <Link href={href}>
        <ArrowLeft data-icon="inline-start" />
        {label}
      </Link>
    </Button>
  );
}
