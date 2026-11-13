import { RequireAlumni } from "@/components/alumni/require-alumni";
import { SurveysList } from "@/components/surveys/surveys-list";

export default function DashboardSurveysPage() {
  return (
    <RequireAlumni
      title="Ankiety dostępne dla zweryfikowanych alumnów"
      description="Po zatwierdzeniu wniosku wypełnisz ankiety klubu i pomożesz planować jego działania."
    >
      <SurveysList />
    </RequireAlumni>
  );
}
