import { Suspense } from "react";
import { AdminAlumniDirectory } from "@/components/admin/admin-alumni-directory";

export default function DashboardAdminAlumniPage() {
  return (
    // useSearchParams needs a Suspense boundary for the page to prerender.
    <Suspense fallback={null}>
      <AdminAlumniDirectory />
    </Suspense>
  );
}
