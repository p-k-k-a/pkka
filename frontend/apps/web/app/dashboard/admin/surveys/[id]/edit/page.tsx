import { SurveyEditor } from "@/components/admin/survey-editor";

type DashboardEditSurveyPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardEditSurveyPage({ params }: DashboardEditSurveyPageProps) {
  const { id } = await params;
  return <SurveyEditor id={id} />;
}
