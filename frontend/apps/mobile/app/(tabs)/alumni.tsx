import { AlumniDirectory } from "@/components/alumni-directory/alumni-directory";
import { RequireAlumni } from "@/components/auth/require-alumni";

export default function AlumniScreen() {
  return (
    <RequireAlumni>
      <AlumniDirectory />
    </RequireAlumni>
  );
}
