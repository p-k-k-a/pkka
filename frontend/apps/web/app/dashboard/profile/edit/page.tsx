import { RequireAlumni } from "@/components/alumni/require-alumni";
import { ProfileEditForm } from "@/components/profile/profile-edit-form";

export default function DashboardProfileEditPage() {
  return (
    <RequireAlumni>
      <ProfileEditForm />
    </RequireAlumni>
  );
}
