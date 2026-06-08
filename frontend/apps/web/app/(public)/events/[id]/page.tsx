import { EventDetail } from "@/components/event-detail";

type Props = {
  params: Promise<{ id: string }>;
};

export default async function EventPage({ params }: Props) {
  const { id } = await params;
  return <EventDetail id={id} />;
}
