"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  CalendarDays,
  Inbox,
  Library,
  Megaphone,
  MessagesSquare,
  Newspaper,
  ShieldCheck,
  Users,
} from "lucide-react";
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
    href: "/dashboard/alumni",
    label: "Alumni",
    icon: Users,
    match: (pathname) => pathname.startsWith("/dashboard/alumni"),
  },
  {
    href: "/dashboard/groups",
    label: "Grupy",
    icon: MessagesSquare,
    match: (pathname) => pathname.startsWith("/dashboard/groups"),
  },
  {
    href: "/dashboard/materials",
    label: "Materiały",
    icon: Library,
    match: (pathname) => pathname.startsWith("/dashboard/materials"),
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
  {
    href: "/dashboard/posts",
    label: "Aktualności",
    icon: Newspaper,
    match: (pathname) => pathname.startsWith("/dashboard/posts"),
  },
  {
    href: "/dashboard/admin/events",
    label: "Wydarzenia",
    icon: CalendarDays,
    match: (pathname) => pathname.startsWith("/dashboard/admin/events"),
  },
  {
    href: "/dashboard/admin/materials",
    label: "Materiały",
    icon: Library,
    match: (pathname) => pathname.startsWith("/dashboard/admin/materials"),
  },
  {
    href: "/dashboard/admin/alumni",
    label: "Alumni",
    icon: Users,
    match: (pathname) => pathname.startsWith("/dashboard/admin/alumni"),
  },
  {
    href: "/dashboard/admin/groups",
    label: "Grupy",
    icon: MessagesSquare,
    match: (pathname) => pathname.startsWith("/dashboard/admin/groups"),
  },
];

export function DashboardSidebar() {
  const pathname = usePathname();
  const { admin, isAuthLoading, isVerified } = useVerificationStatus();

  // Verification is offered until it succeeds, alumni-only pages only after it does.
  const items = admin
    ? adminItems
    : userItems.filter((item) => {
        if (item.href === "/dashboard/verification") return !isVerified;
        if (
          item.href === "/dashboard/alumni" ||
          item.href === "/dashboard/groups" ||
          item.href === "/dashboard/materials"
        ) {
          return isVerified;
        }
        return true;
      });

  return (
    <aside className="bg-muted border-background flex shrink-0 overflow-x-auto border-b md:sticky md:top-0 md:h-full md:w-56 md:flex-col md:self-stretch md:overflow-y-auto md:border-r md:border-b-0">
      <nav className="font-heading flex gap-1 px-4 py-2 md:flex-col md:p-4">
        {isAuthLoading ? (
          <>
            <Skeleton className="h-10 w-28 rounded-lg md:w-full" />
            <Skeleton className="h-10 w-28 rounded-lg md:w-full" />
            <Skeleton className="h-10 w-28 rounded-lg md:w-full" />
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
                  "flex shrink-0 items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium whitespace-nowrap transition-colors",
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
