"use client";

import { DashboardAuthGuard } from "@/components/dashboard/dashboard-auth-guard";
import { DashboardHeader } from "@/components/dashboard/dashboard-header";
import { DashboardSidebar } from "@/components/dashboard/dashboard-sidebar";

export function DashboardShell({ children }: { children: React.ReactNode }) {
  return (
    <DashboardAuthGuard>
      <div className="bg-background flex min-h-screen flex-col">
        <DashboardHeader />
        <div className="flex min-h-0 flex-1">
          <DashboardSidebar />
          <main className="min-w-0 flex-1 overflow-y-auto">{children}</main>
        </div>
      </div>
    </DashboardAuthGuard>
  );
}
