import { RequireAlumni } from "@/components/alumni/require-alumni";
import { SurveyFill } from "@/components/surveys/survey-fill";

type DashboardSurveyPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardSurveyPage({ params }: DashboardSurveyPageProps) {
  const { id } = await params;
  return (
    <RequireAlumni
      title="Ankiety dostępne dla zweryfikowanych alumnów"
      description="Po zatwierdzeniu wniosku wypełnisz ankiety klubu i pomożesz planować jego działania."
    >
      <SurveyFill id={id} />
    </RequireAlumni>
  );
}
