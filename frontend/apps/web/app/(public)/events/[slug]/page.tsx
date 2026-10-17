import { EventDetail } from "@/components/event-detail";

type EventPageProps = {
  params: Promise<{ id: string }>;
};

export default async function EventPage({ params }: EventPageProps) {
  const { id } = await params;
  return <EventDetail id={id} />;
}
