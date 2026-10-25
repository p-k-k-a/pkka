import { RequireAlumni } from "@/components/alumni/require-alumni";
import { ProfileContent } from "@/components/profile/profile-content";

export default function DashboardProfilePage() {
  return (
    <RequireAlumni>
      <ProfileContent />
    </RequireAlumni>
  );
}
