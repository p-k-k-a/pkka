import { AnnouncementDetail } from "@/components/announcement-detail";

type AnnouncementPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function AnnouncementPage({ params }: AnnouncementPageProps) {
  const { slug } = await params;
  return <AnnouncementDetail slug={slug} />;
}
