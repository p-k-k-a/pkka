"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/lib/auth-context";

export function DashboardHome() {
  const { user, logout } = useAuth();

  return (
    <div className="mx-auto flex min-h-screen max-w-md items-center px-6 py-12">
      <Card className="border-border/70 w-full rounded-[24px] shadow-sm">
        <CardHeader className="space-y-2">
          <CardTitle className="text-2xl font-extrabold tracking-tight">Zalogowany użytkownik</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <dl className="space-y-3 text-sm">
            {user?.name ? (
              <div>
                <dt className="text-muted-foreground">Imię i nazwisko</dt>
                <dd className="font-medium">{user.name}</dd>
              </div>
            ) : null}
            <div>
              <dt className="text-muted-foreground">Login</dt>
              <dd className="font-medium">{user?.username}</dd>
            </div>
            {user?.email ? (
              <div>
                <dt className="text-muted-foreground">E-mail</dt>
                <dd className="font-medium">{user.email}</dd>
              </div>
            ) : null}
            {user?.roles?.length ? (
              <div>
                <dt className="text-muted-foreground">Role</dt>
                <dd className="font-medium">{user.roles.join(", ")}</dd>
              </div>
            ) : null}
          </dl>

          <Button variant="destructive" className="w-full rounded-xl" onClick={logout}>
            Wyloguj się
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
