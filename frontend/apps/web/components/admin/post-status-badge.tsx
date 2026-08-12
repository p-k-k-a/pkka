import { Badge } from "@/components/ui/badge";

type PostStatusBadgeProps = {
  status: "DRAFT" | "PUBLISHED";
};

export function PostStatusBadge({ status }: PostStatusBadgeProps) {
  if (status === "PUBLISHED") {
    return <Badge className="bg-accent/10 text-accent">Opublikowany</Badge>;
  }
  return <Badge variant="outline">Szkic</Badge>;
}
