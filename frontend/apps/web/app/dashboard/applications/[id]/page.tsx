import { ApplicationDetail } from "@/components/admin/application-detail";

type DashboardApplicationPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardApplicationPage({
  params,
}: DashboardApplicationPageProps) {
  const { id } = await params;
  return <ApplicationDetail id={id} />;
}
