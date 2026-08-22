import { PostEditor } from "@/components/admin/post-editor";

type DashboardEditPostPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardEditPostPage({ params }: DashboardEditPostPageProps) {
  const { id } = await params;
  return <PostEditor id={id} />;
}
