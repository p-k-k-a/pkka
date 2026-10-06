import { Suspense } from "react";
import { AlumniDirectory } from "@/components/alumni/alumni-directory";
import { RequireAlumni } from "@/components/alumni/require-alumni";

export default function DashboardAlumniPage() {
  return (
    <RequireAlumni>
      {/* useSearchParams needs a Suspense boundary for the page to prerender. */}
      <Suspense fallback={null}>
        <AlumniDirectory />
      </Suspense>
    </RequireAlumni>
  );
}
