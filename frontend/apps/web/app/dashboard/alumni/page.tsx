import { AlumniDirectory } from "@/components/alumni/alumni-directory";
import { RequireAlumni } from "@/components/alumni/require-alumni";

export default function DashboardAlumniPage() {
  return (
    <RequireAlumni>
      <AlumniDirectory />
    </RequireAlumni>
  );
}
