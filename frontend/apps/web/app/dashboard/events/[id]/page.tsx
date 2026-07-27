import { EventDetail } from "@/components/event-detail";

type DashboardEventPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardEventPage({ params }: DashboardEventPageProps) {
  const { id } = await params;
  return <EventDetail id={id} variant="dashboard" />;
}
