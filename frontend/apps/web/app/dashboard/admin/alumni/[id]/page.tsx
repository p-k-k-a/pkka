import { AdminAlumniProfileContent } from "@/components/alumni/alumni-profile-content";

type DashboardAdminAlumnPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardAdminAlumnPage({ params }: DashboardAdminAlumnPageProps) {
  const { id } = await params;
  return <AdminAlumniProfileContent id={id} />;
}
