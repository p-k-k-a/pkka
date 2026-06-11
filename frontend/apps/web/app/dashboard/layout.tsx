import Link from "next/link";
import { PublicHeaderAuth } from "@/components/layout/public-header-auth";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="border-border/80 bg-background/95 sticky top-0 z-50 border-b px-6 py-4 backdrop-blur">
        <nav className="flex items-center justify-between">
          <Link href="/dashboard" className="text-foreground text-3xl font-extrabold tracking-tighter">
            PKKA
          </Link>
          <PublicHeaderAuth />
        </nav>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
