"use client";

import { AppGuide } from "@/components/app-guide";
import { useVerificationStatus } from "@/lib/use-verification-status";

export default function DashboardInfoPage() {
  const { admin } = useVerificationStatus();
  return <AppGuide showAdmin={admin} />;
}
