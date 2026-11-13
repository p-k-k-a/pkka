import { SurveyResults } from "@/components/admin/survey-results";

type DashboardSurveyResultsPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardSurveyResultsPage({
  params,
}: DashboardSurveyResultsPageProps) {
  const { id } = await params;
  return <SurveyResults id={id} />;
}
