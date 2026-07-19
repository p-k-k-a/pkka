import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { PublicSessionRedirect } from "@/components/auth/public-session-redirect";
import { NavLink } from "@/components/nav-link";
import { PublicHeaderAuth } from "@/components/layout/public-header-auth";

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
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

      <main className="flex-1">
        <PublicSessionRedirect>{children}</PublicSessionRedirect>
      </main>

      <footer className="bg-navy text-white-text px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto flex max-w-[1280px] flex-col gap-8 md:flex-row md:items-end md:justify-between">
          <div className="space-y-3">
            <p className="font-heading text-[23px] font-semibold tracking-tight md:text-[28px]">
              PKKA
            </p>
            <p className="text-white-text/80 max-w-md text-sm leading-relaxed md:text-base">
              Klub Alumnów Wydziału Informatyki AGH — społeczność absolwentów, mentoring i
              networking.
            </p>
          </div>
          <div className="space-y-2 text-sm md:text-right">
            <Link
              href="/info"
              className="font-heading text-white-text inline-flex items-center gap-2 hover:underline"
            >
              Dowiedz się więcej
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
            <p className="text-white-text/70">© 2026 Klub Alumnów WI AGH</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
