import { SurveysList } from "@/components/surveys/surveys-list";

// Open to every signed-in user; the API lists only the surveys meant for them.
export default function DashboardSurveysPage() {
  return <SurveysList />;
}
