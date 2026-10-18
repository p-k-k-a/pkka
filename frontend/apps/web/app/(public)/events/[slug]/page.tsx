import { EventDetail } from "@/components/event-detail";

type EventPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function EventPage({ params }: EventPageProps) {
  const { slug } = await params;
  return <EventDetail slug={slug} />;
}
