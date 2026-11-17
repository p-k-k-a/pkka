import { SurveyFill } from "@/components/surveys/survey-fill";

type DashboardSurveyPageProps = {
  params: Promise<{ id: string }>;
};

// Open to every signed-in user; a survey meant for others answers as not found.
export default async function DashboardSurveyPage({ params }: DashboardSurveyPageProps) {
  const { id } = await params;
  return <SurveyFill id={id} />;
}
