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
import {
  ApplicationStatus,
  TopicGroupRequestStatus,
  useListAdminApplications,
  useListAdminTopicGroupRequests,
} from "@pkka/api";
import { cn } from "@/lib/utils";
import { useVerificationStatus } from "@/lib/use-verification-status";
import { Skeleton } from "@/components/ui/skeleton";

const APPLICATIONS_HREF = "/dashboard/applications";
const ADMIN_GROUPS_HREF = "/dashboard/admin/groups";

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
    label: "Grupy tematyczne",
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
    href: APPLICATIONS_HREF,
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
    href: ADMIN_GROUPS_HREF,
    label: "Grupy tematyczne",
    icon: MessagesSquare,
    match: (pathname) => pathname.startsWith("/dashboard/admin/groups"),
  },
];

function usePendingCounts(admin: boolean): Record<string, number> {
  const adminOnly = { query: { enabled: admin } };
  const { data: applications } = useListAdminApplications(
    { status: ApplicationStatus.UNDER_REVIEW, size: 1 },
    adminOnly,
  );
  const { data: groupRequests } = useListAdminTopicGroupRequests(
    { status: TopicGroupRequestStatus.UNDER_REVIEW, size: 1 },
    adminOnly,
  );
  return {
    [APPLICATIONS_HREF]: applications?.data.totalElements ?? 0,
    [ADMIN_GROUPS_HREF]: groupRequests?.data.totalElements ?? 0,
  };
}

export function DashboardSidebar() {
  const pathname = usePathname();
  const { admin, isAuthLoading, isVerified } = useVerificationStatus();
  const pendingCounts = usePendingCounts(admin);

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
    <aside className="bg-muted border-background sticky top-0 flex h-full w-56 shrink-0 flex-col self-stretch overflow-y-auto border-r">
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
            const pendingCount = pendingCounts[href] ?? 0;

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
                <span className="relative shrink-0">
                  <Icon className="size-4" aria-hidden="true" />
                  {pendingCount > 0 ? (
                    <span
                      aria-label={`Oczekujące na decyzję: ${pendingCount}`}
                      className="bg-primary text-primary-foreground ring-muted absolute -top-2 -right-2.5 flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[10px] leading-none font-bold ring-2"
                    >
                      {pendingCount > 99 ? "99+" : pendingCount}
                    </span>
                  ) : null}
                </span>
                {label}
              </Link>
            );
          })
        )}
      </nav>
    </aside>
  );
}
