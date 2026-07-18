import { DashboardHeader } from "@/components/dashboard/dashboard-header";
import { DashboardSidebar } from "@/components/dashboard/dashboard-sidebar";

export function DashboardShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-background flex h-dvh flex-col overflow-hidden">
      <DashboardHeader />
      <div className="flex min-h-0 flex-1">
        <DashboardSidebar />
        <main className="min-w-0 flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
