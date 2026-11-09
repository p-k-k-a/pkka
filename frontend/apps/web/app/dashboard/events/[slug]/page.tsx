import { EventDetail } from "@/components/event-detail";

type DashboardEventPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function DashboardEventPage({ params }: DashboardEventPageProps) {
  const { slug } = await params;
  return <EventDetail slug={slug} variant="dashboard" />;
}
