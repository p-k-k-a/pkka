"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ALUMNI_CONTACTS_HREF, ALUMNI_DIRECTORY_HREF } from "@/lib/alumni-search-params";
import { cn } from "@/lib/utils";

const TABS = [
  { href: ALUMNI_DIRECTORY_HREF, label: "Katalog" },
  { href: ALUMNI_CONTACTS_HREF, label: "Moje kontakty" },
];

export function AlumniSectionTabs() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Widok sekcji Alumni"
      className="border-muted-foreground/25 dark:border-input flex w-fit gap-1 border-b"
    >
      {TABS.map(({ href, label }) => {
        const isActive = pathname === href;
        return (
          <Link
            key={href}
            href={href}
            aria-current={isActive ? "page" : undefined}
            className={cn(
              "font-heading focus-visible:ring-ring -mb-px rounded-t-md border-b-2 px-4 py-2 text-base transition-colors focus-visible:ring-2 focus-visible:outline-none",
              isActive
                ? "border-primary text-foreground font-semibold"
                : "text-muted-foreground hover:text-foreground border-transparent font-medium",
            )}
          >
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
