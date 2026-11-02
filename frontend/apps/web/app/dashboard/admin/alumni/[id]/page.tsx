import { AdminAlumniProfile } from "@/components/admin/admin-alumni-profile";

type DashboardAdminAlumnPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardAdminAlumnPage({ params }: DashboardAdminAlumnPageProps) {
  const { id } = await params;
  return <AdminAlumniProfile id={id} />;
}
