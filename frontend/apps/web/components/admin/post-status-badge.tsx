import { PostStatus } from "@pkka/api";
import { Badge } from "@/components/ui/badge";

type PostStatusBadgeProps = {
  status: PostStatus;
};

export function PostStatusBadge({ status }: PostStatusBadgeProps) {
  if (status === PostStatus.PUBLISHED) {
    return <Badge>Opublikowany</Badge>;
  }
  return <Badge variant="outline">Szkic</Badge>;
}
