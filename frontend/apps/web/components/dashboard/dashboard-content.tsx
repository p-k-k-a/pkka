"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { displayRoles } from "@/lib/role-labels";

export function DashboardContent() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/");
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4">
        <Skeleton className="h-96 w-full max-w-md rounded-2xl" />
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return null;
  }

  const fullName = [user.firstName, user.lastName].filter(Boolean).join(" ");
  const displayName = fullName || user.preferredUsername || "?";
  const roles = displayRoles(user.roles ?? []);

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="border-border bg-card w-full max-w-md rounded-2xl border p-8 shadow-sm">
        <div className="mb-8 text-center">
          <div className="bg-muted mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full">
            <span className="text-muted-foreground text-3xl font-bold">
              {displayName.charAt(0).toUpperCase()}
            </span>
          </div>
          <h1 className="text-foreground text-2xl font-bold">{displayName}</h1>
          {user.email && <p className="text-muted-foreground mt-1 text-sm">{user.email}</p>}
        </div>

        <div className="mb-8 space-y-3">
          {user.preferredUsername && (
            <InfoRow label="Nazwa użytkownika" value={user.preferredUsername} />
          )}
          {fullName && <InfoRow label="Imię i nazwisko" value={fullName} />}
          {user.email && <InfoRow label="Email" value={user.email} />}
          <div className="bg-muted flex items-start justify-between gap-4 rounded-lg px-4 py-3">
            <span className="text-muted-foreground text-sm font-medium">Role</span>
            {roles.length > 0 ? (
              <div className="flex flex-wrap justify-end gap-1.5">
                {roles.map((role) => (
                  <span
                    key={role}
                    className="bg-secondary text-secondary-foreground inline-block rounded-lg px-2.5 py-0.5 text-xs font-medium"
                  >
                    {role}
                  </span>
                ))}
              </div>
            ) : (
              <span className="text-muted-foreground text-sm">Brak przypisanych ról</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-muted flex items-center justify-between gap-4 rounded-lg px-4 py-3">
      <span className="text-muted-foreground text-sm font-medium">{label}</span>
      <span className="text-foreground text-sm font-semibold">{value}</span>
    </div>
  );
}
