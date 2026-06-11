"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CalendarDays, Megaphone, ShieldCheck } from "lucide-react";
import { cn } from "@/lib/utils";

type SidebarItem = {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  match: (pathname: string) => boolean;
};

const items: SidebarItem[] = [
  {
    href: "/dashboard",
    label: "Aktualności",
    icon: Megaphone,
    match: (pathname) => pathname === "/dashboard" || pathname.startsWith("/dashboard/announcements"),
  },
  {
    href: "/dashboard/events",
    label: "Wydarzenia",
    icon: CalendarDays,
    match: (pathname) => pathname.startsWith("/dashboard/events"),
  },
  {
    href: "/dashboard/verification",
    label: "Weryfikacja",
    icon: ShieldCheck,
    match: (pathname) => pathname.startsWith("/dashboard/verification"),
  },
];

export function DashboardSidebar() {
  const pathname = usePathname();

  return (
    <aside className="border-border bg-muted/30 w-56 shrink-0 border-r">
      <nav className="flex flex-col gap-1 p-4">
        {items.map(({ href, label, icon: Icon, match }) => {
          const isActive = match(pathname);

          return (
            <Link
              key={href}
              href={href}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                isActive
                  ? "bg-background text-foreground shadow-sm"
                  : "text-muted-foreground hover:bg-background/60 hover:text-foreground",
              )}
            >
              <Icon className="size-4 shrink-0" aria-hidden="true" />
              {label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
