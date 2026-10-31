import { Suspense } from "react";
import { AlumniDirectory } from "@/components/alumni/alumni-directory";

export default function DashboardAdminAlumniPage() {
  return (
    // useSearchParams needs a Suspense boundary for the page to prerender.
    <Suspense fallback={null}>
      <AlumniDirectory variant="admin" />
    </Suspense>
  );
}
