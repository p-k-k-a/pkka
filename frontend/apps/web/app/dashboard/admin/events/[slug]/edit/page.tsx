import { EventEditor } from "@/components/admin/event-editor";

type DashboardEditEventPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function DashboardEditEventPage({ params }: DashboardEditEventPageProps) {
  const { slug } = await params;
  return <EventEditor slug={slug} />;
}
