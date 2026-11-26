import { RequireAlumni } from "@/components/alumni/require-alumni";
import { NewGroupContent } from "@/components/topic-groups/new-group-content";

export default function DashboardNewGroupPage() {
  return (
    <RequireAlumni>
      <NewGroupContent />
    </RequireAlumni>
  );
}
