import { AnnouncementDetail } from "@/components/announcement-detail";

type Props = {
  params: Promise<{ slug: string }>;
};

export default async function AnnouncementPage({ params }: Props) {
  const { slug } = await params;
  return <AnnouncementDetail slug={slug} />;
}
