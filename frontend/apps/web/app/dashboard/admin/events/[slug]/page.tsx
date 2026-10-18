import { AdminEventOverview } from "@/components/admin/statistics/admin-event-overview";

type DashboardAdminEventPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function DashboardAdminEventPage({ params }: DashboardAdminEventPageProps) {
  const { slug } = await params;
  return <AdminEventOverview slug={slug} />;
}
