import { AnnouncementDetail } from "@/components/announcement-detail";

type DashboardAnnouncementPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function DashboardAnnouncementPage({
  params,
}: DashboardAnnouncementPageProps) {
  const { slug } = await params;
  return <AnnouncementDetail slug={slug} backHref="/dashboard" />;
}
