"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

type NavLinkProps = {
  href: string;
  children: React.ReactNode;
  activePaths?: string[];
};

function isPathActive(pathname: string, path: string) {
  if (pathname === path) return true;
  if (path === "/") return false;
  return pathname.startsWith(`${path}/`);
}

export function NavLink({ href, children, activePaths }: NavLinkProps) {
  const pathname = usePathname();
  const paths = activePaths ?? [href];

  const isActive = paths.some((path) => isPathActive(pathname, path));

  return (
    <Link
      href={href}
      aria-current={isActive ? "page" : undefined}
      className={cn(
        "font-heading text-base font-medium transition-colors",
        isActive ? "text-foreground" : "text-muted-foreground hover:text-foreground",
      )}
    >
      {children}
    </Link>
  );
}
