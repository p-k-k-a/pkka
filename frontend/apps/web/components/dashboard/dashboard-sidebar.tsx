"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  CalendarDays,
  ClipboardList,
  Inbox,
  Library,
  Megaphone,
  Newspaper,
  ShieldCheck,
  Users,
} from "lucide-react";
import { useListActiveSurveys } from "@pkka/api";
import { SURVEYS_PATH } from "@/lib/survey-paths";
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
    href: "/dashboard/materials",
    label: "Materiały",
    icon: Library,
    match: (pathname) => pathname.startsWith("/dashboard/materials"),
  },
  {
    href: SURVEYS_PATH,
    label: "Ankiety",
    icon: ClipboardList,
    match: (pathname) => pathname.startsWith("/dashboard/surveys"),
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
    href: "/dashboard/admin/surveys",
    label: "Ankiety",
    icon: ClipboardList,
    match: (pathname) => pathname.startsWith("/dashboard/admin/surveys"),
  },
  {
    href: "/dashboard/admin/alumni",
    label: "Alumni",
    icon: Users,
    match: (pathname) => pathname.startsWith("/dashboard/admin/alumni"),
  },
];

const ALUMNI_ONLY_HREFS = new Set(["/dashboard/alumni", "/dashboard/materials", SURVEYS_PATH]);

export function DashboardSidebar() {
  const pathname = usePathname();
  const { admin, isAuthLoading, isVerified } = useVerificationStatus();
  // Only the total is needed, to show how many surveys are waiting.
  const surveysQuery = useListActiveSurveys(
    { size: 1 },
    { query: { enabled: isVerified && !admin } },
  );
  const waitingSurveys =
    surveysQuery.data?.status === 200 ? (surveysQuery.data.data.totalElements ?? 0) : 0;

  // Verification is offered until it succeeds, alumni-only pages only after it does.
  const items = admin
    ? adminItems
    : userItems.filter((item) => {
        if (item.href === "/dashboard/verification") return !isVerified;
        if (ALUMNI_ONLY_HREFS.has(item.href)) return isVerified;
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
                {href === SURVEYS_PATH && waitingSurveys > 0 ? (
                  <span className="bg-primary text-primary-foreground ml-auto rounded-full px-2 py-0.5 text-xs leading-none font-semibold tabular-nums">
                    <span className="sr-only">Do wypełnienia: </span>
                    {waitingSurveys}
                  </span>
                ) : null}
              </Link>
            );
          })
        )}
      </nav>
    </aside>
  );
}
