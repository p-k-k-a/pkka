"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@/lib/roles";
import { AnnouncementsSection } from "@/components/announcements-section";
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
      <div className="mx-auto max-w-7xl px-6 py-12 md:py-16">
        <Skeleton className="mb-10 h-10 w-64" />
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton
              key={i}
              className={`h-64 rounded-[24px] ${i === 0 ? "md:col-span-2 md:h-80" : ""}`}
            />
          ))}
        </div>
      </div>
    );
  }

  return (
    <AnnouncementsSection title="Aktualności" announcementBasePath="/dashboard/announcements" />
  );
}
