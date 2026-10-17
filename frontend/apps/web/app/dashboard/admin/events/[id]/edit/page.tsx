import { EventEditor } from "@/components/admin/event-editor";

type DashboardEditEventPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardEditEventPage({ params }: DashboardEditEventPageProps) {
  const { id } = await params;
  return <EventEditor id={id} />;
}
