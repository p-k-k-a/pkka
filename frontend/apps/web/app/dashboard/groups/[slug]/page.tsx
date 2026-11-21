import { RequireAlumni } from "@/components/alumni/require-alumni";
import { TopicGroupDetails } from "@/components/topic-groups/group-details";

type DashboardGroupPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function DashboardGroupPage({ params }: DashboardGroupPageProps) {
  const { slug } = await params;
  return (
    <RequireAlumni>
      <TopicGroupDetails slug={slug} />
    </RequireAlumni>
  );
}
