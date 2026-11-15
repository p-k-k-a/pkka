import { Suspense } from "react";
import { MaterialsLibrary } from "@/components/materials/materials-library";

export default function DashboardMaterialsPage() {
  return (
    // useSearchParams needs a Suspense boundary for the page to prerender.
    <Suspense fallback={null}>
      <MaterialsLibrary />
    </Suspense>
  );
}
