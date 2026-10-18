import { AlumniProfileContent } from "@/components/alumni/alumni-profile-content";
import { RequireAlumni } from "@/components/alumni/require-alumni";

type DashboardAlumnPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardAlumnPage({ params }: DashboardAlumnPageProps) {
  const { id } = await params;
  return (
    <RequireAlumni>
      <AlumniProfileContent id={id} />
    </RequireAlumni>
  );
}
