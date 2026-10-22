import Link from "next/link";
import { NavLink } from "@/components/nav-link";
import { PublicHeaderAuth } from "@/components/layout/public-header-auth";

export function PublicHeader() {
  return (
    <header className="bg-muted sticky top-0 z-50 px-4 py-4 md:px-10">
      <nav className="mx-auto flex max-w-[1280px] items-center">
        <div className="flex flex-1 justify-start">
          <Link
            href="/"
            className="font-heading text-foreground text-[23px] font-semibold tracking-tight md:text-[28px]"
          >
            PKKA
          </Link>
        </div>

        <div className="font-heading flex gap-6 text-base md:gap-8">
          <NavLink href="/" activePaths={["/", "/announcements"]}>
            Aktualności
          </NavLink>
          <NavLink href="/events">Wydarzenia</NavLink>
          <NavLink href="/info">O klubie</NavLink>
        </div>

        <div className="flex flex-1 items-center justify-end">
          <PublicHeaderAuth />
        </div>
      </nav>
    </header>
  );
}
