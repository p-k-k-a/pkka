import { RequireAlumni } from "@/components/alumni/require-alumni";
import { TopicGroupsContent } from "@/components/topic-groups/topic-groups-content";

export default function DashboardGroupsPage() {
  return (
    <RequireAlumni>
      <TopicGroupsContent />
    </RequireAlumni>
  );
}
