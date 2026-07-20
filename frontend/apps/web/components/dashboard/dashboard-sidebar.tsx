"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CalendarDays, Inbox, Megaphone, ShieldCheck } from "lucide-react";
import { cn } from "@/lib/utils";
import { useVerificationStatus } from "@/lib/use-verification-status";
import { Skeleton } from "@/components/ui/skeleton";

type SidebarItem = {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  match: (pathname: string) => boolean;
};

const userItems: SidebarItem[] = [
  {
    href: "/dashboard",
    label: "Aktualności",
    icon: Megaphone,
    match: (pathname) =>
      pathname === "/dashboard" || pathname.startsWith("/dashboard/announcements"),
  },
  {
    href: "/dashboard/events",
    label: "Wydarzenia",
    icon: CalendarDays,
    match: (pathname) => pathname.startsWith("/dashboard/events"),
  },
  {
    href: "/dashboard/verification",
    label: "Zweryfikuj się",
    icon: ShieldCheck,
    match: (pathname) => pathname.startsWith("/dashboard/verification"),
  },
];

const adminItems: SidebarItem[] = [
  {
    href: "/dashboard/applications",
    label: "Wnioski",
    icon: Inbox,
    match: (pathname) => pathname.startsWith("/dashboard/applications"),
  },
];

export function DashboardSidebar() {
  const pathname = usePathname();
  const { admin, isAuthLoading, isVerified } = useVerificationStatus();

  const items = admin
    ? adminItems
    : userItems.filter((item) => item.href !== "/dashboard/verification" || !isVerified);

  return (
    <aside className="bg-muted sticky top-0 flex h-full w-56 shrink-0 flex-col self-stretch overflow-y-auto">
      <nav className="font-heading flex flex-col gap-1 p-4">
        {isAuthLoading ? (
          <>
            <Skeleton className="h-10 w-full rounded-lg" />
            <Skeleton className="h-10 w-full rounded-lg" />
            <Skeleton className="h-10 w-full rounded-lg" />
          </>
        ) : (
          items.map(({ href, label, icon: Icon, match }) => {
            const isActive = match(pathname);

            return (
              <Link
                key={href}
                href={href}
                aria-current={isActive ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-background text-foreground"
                    : "text-muted-foreground hover:bg-background/60 hover:text-foreground",
                )}
              >
                <Icon className="size-4 shrink-0" aria-hidden="true" />
                {label}
              </Link>
            );
          })
        )}
      </nav>
    </aside>
  );
}
