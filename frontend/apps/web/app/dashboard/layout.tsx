export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return <div className="flex min-h-screen flex-col bg-white dark:bg-white">{children}</div>;
}
