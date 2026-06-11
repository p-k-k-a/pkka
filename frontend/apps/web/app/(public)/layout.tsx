import Link from "next/link";
import { PublicSessionRedirect } from "@/components/auth/public-session-redirect";
import { NavLink } from "@/components/nav-link";
import { PublicHeaderAuth } from "@/components/layout/public-header-auth";

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-border/80 bg-background/95 sticky top-0 z-50 border-b px-6 py-4 backdrop-blur">
        <nav className="flex items-center">
          <div className="flex flex-1 justify-start">
            <Link href="/" className="text-foreground text-3xl font-extrabold tracking-tighter">
              PKKA
            </Link>
          </div>

          <div className="flex gap-8">
            <NavLink href="/" activePaths={["/", "/announcements"]}>
              Aktualności
            </NavLink>
            <NavLink href="/events">Wydarzenia</NavLink>
          </div>

          <div className="flex flex-1 items-center justify-end">
            <PublicHeaderAuth />
          </div>
        </nav>
      </header>

      <main className="flex-1">
        <PublicSessionRedirect>{children}</PublicSessionRedirect>
      </main>

      <footer className="text-muted-foreground border-t px-6 py-4 text-center text-sm">
        © 2026 Klub Alumna WI AGH
      </footer>
    </div>
  );
}
