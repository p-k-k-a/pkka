import { AdminEventOverview } from "@/components/admin/statistics/admin-event-overview";

type DashboardAdminEventPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardAdminEventPage({ params }: DashboardAdminEventPageProps) {
  const { id } = await params;
  return <AdminEventOverview id={id} />;
}
