"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@pkka/domain";
import { AnnouncementsSection, AnnouncementsSkeleton } from "@/components/announcements-section";
import { Skeleton } from "@/components/ui/skeleton";

export function DashboardHome() {
  const router = useRouter();
  const { user, isLoading } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && admin) {
      router.replace("/dashboard/applications");
    }
  }, [admin, isLoading, router]);

  if (isLoading || admin) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <div className="mx-auto max-w-[1280px]">
          <Skeleton className="mb-10 h-10 w-64 rounded-lg" />
          <AnnouncementsSkeleton />
        </div>
      </div>
    );
  }

  return <AnnouncementsSection variant="dashboard" />;
}
