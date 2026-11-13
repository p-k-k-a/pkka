import { Suspense } from "react";
import { RequireAlumni } from "@/components/alumni/require-alumni";
import { MaterialsLibrary } from "@/components/materials/materials-library";

export default function DashboardMaterialsPage() {
  return (
    <RequireAlumni
      title="Materiały dostępne dla zweryfikowanych alumnów"
      description="Po zatwierdzeniu wniosku zobaczysz nagrania i prezentacje z wydarzeń klubu."
    >
      {/* useSearchParams needs a Suspense boundary for the page to prerender. */}
      <Suspense fallback={null}>
        <MaterialsLibrary />
      </Suspense>
    </RequireAlumni>
  );
}
