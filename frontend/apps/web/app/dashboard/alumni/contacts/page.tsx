import { AlumniContacts } from "@/components/alumni/alumni-contacts";
import { RequireAlumni } from "@/components/alumni/require-alumni";

export default function DashboardAlumniContactsPage() {
  return (
    <RequireAlumni>
      <AlumniContacts />
    </RequireAlumni>
  );
}
